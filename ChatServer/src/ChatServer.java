import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

/**
 * Multithreading has been tried but the fatal shortage is the server cannot shutdown in a
 * peaceful way once receive "KILL_SERVICE" message. Thus this program applies non-blocking
 * IO to get rid of IO wait when server is waiting for next incoming socket. With multiplexing,
 * server thread can response to all clients any time without IO blocking.
 */
public class ChatServer {

    // A selector let program know which socket is active.
    private Selector selector;

    // The following 3 members are for converting messages.
    private ByteBuffer byteBuffer;

    private Charset charset;

    private CharsetDecoder decoder;

    private String serverIP;

    private String serverPort;

    // Use ChatService to manage users.
    private ChatService chatService;

    public ChatServer(int port) throws IOException {

        // Initialize members.
        byteBuffer = ByteBuffer.allocate(2048);

        charset = Charset.forName("UTF-8");

        decoder = charset.newDecoder();

        // Create server socket.
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        selector = Selector.open();

        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        serverIP = Inet4Address.getLocalHost().getHostAddress();

        serverPort = String.valueOf(serverSocketChannel.socket().getLocalPort());

        // Set server socket non-blocking.
        serverSocketChannel.configureBlocking(false);

        // Add server socket to selector, then socket.accept() won't block the thread.
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        chatService = new ChatService();
    }

    // Once chat server starts, loops until receive "KILL_SERVICE" signal.
    public void loop() throws IOException
    {
        String processedInfo;

        // Set a flag to let server know the end point.
        boolean stopFlag = false;

        while(!stopFlag)
        {
            // In each loop, let select tell program which client is active.
            selector.select();

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while(iterator.hasNext())
            {
                SelectionKey key = iterator.next();

                iterator.remove();

                // Process active clients.
                processedInfo = process(key);

                // Take special action if received special command.
                if(processedInfo.equals("KILL_SERVICE"))
                {
                    stopFlag = true;

                    break;
                }
                // Client is leaving the server.
                else if(processedInfo.equals("DISCONNECT"))
                {
                    key.cancel();
                }
            }
        }

        Iterator<SelectionKey> iterator = selector.keys().iterator();

        // Close all socket before shutdown the server.
        while(iterator.hasNext())
        {
            SelectionKey key = iterator.next();

            // Close server socket.
            if(key.isAcceptable())
            {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                serverSocketChannel.close();
            }
            // Close client socket.
            else
            {
                SocketChannel socketChannel = (SocketChannel) key.channel();

                socketChannel.close();
            }
        }

        // Close selector.
        selector.close();
    }

    // Process command from client.
    public String process(SelectionKey key) throws IOException {

        byteBuffer.clear();

        // A new client comes in.
        if(key.isAcceptable())
        {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

            SocketChannel channel = serverSocketChannel.accept();

            // Set non-blocking.
            channel.configureBlocking(false);

            // Add to selector.
            channel.register(selector, SelectionKey.OP_READ);
        }
        // Client sent a message.
        else if(key.isReadable())
        {
            SocketChannel channel = (SocketChannel) key.channel();

            // Read the message.
            int count = channel.read(byteBuffer);

            if(count > 0)
            {
                byteBuffer.flip();

                String msg = decoder.decode(byteBuffer).toString().trim();

                byteBuffer.clear();

                // Let caller handles special command.
                if(msg.equals("KILL_SERVICE"))
                {
                    return msg;
                }
                else
                {
                    // Let sub-function handles concrete command.
                    return handleMessage(channel, msg);
                }
            }
            else
            {
                channel.close();
            }
        }

        return "";
    }

    // Handle command.
    public String handleMessage(SocketChannel channel, String msg) throws IOException {

        String echoMsg = "", returnMsg = "", broadcastMsg = "";

        boolean broadcastFlag = false;

        int roomId = -1, userId = -1;

        System.out.println("--------");
        System.out.println("From: " + channel.socket().getInetAddress());
        System.out.println(msg);
        System.out.println("--------");

        // Server should echo a corresponding message for each command.
        // Handle "HELO text" command.
        if(msg.startsWith("HELO"))
        {
            // Form echo message.
            echoMsg += msg;
            echoMsg += "\nIP:";
            echoMsg += serverIP;
            echoMsg += "\nPort:";
            echoMsg += serverPort;
            echoMsg += "\nStudentID:17301984\n";
        }
        // Handle "JOIN_CHATROOM" command.
        else if(msg.startsWith("JOIN_CHATROOM"))
        {
            String[] msgSeg = msg.split("\n");

            String username = msgSeg[3].split(" ")[1];

            // Get new id for new user.
            userId = chatService.generateUserId(username);

            // The username is existed.
            if(userId == -1)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(-1, "Username existed").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

            // Record this user.
            roomId = chatService.joinRoom(msgSeg[0].split(" ")[1], userId, channel);

            // Form echo message.
            echoMsg += "JOINED_CHATROOM: " + msgSeg[0].split(" ")[1];
            echoMsg += "\nSERVER_IP: " + serverIP;
            echoMsg += "\nPORT: " + serverPort;
            echoMsg += "\nROOM_REF: " + roomId;
            echoMsg += "\nJOIN_ID: " + userId + "\n";

            broadcastMsg = "CHAT: " + roomId;
            broadcastMsg += "\nCLIENT_NAME: " + username;
            broadcastMsg += "\nMESSAGE: " + username +" has joined this chatroom.\n\n";

            broadcastFlag = true;
        }
        // Handle "LEAVE_CHATROOM" command.
        else if(msg.startsWith("LEAVE_CHATROOM"))
        {
            String[] msgSeg = msg.split("\n");

            userId = Integer.valueOf(msgSeg[1].split(" ")[1]);

            String username = msgSeg[2].split(" ")[1];

            if(userId < 0)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(userId, "User doesn't exit").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

            roomId = new Integer(msgSeg[0].split(" ")[1]);

            int code = chatService.leaveRoom(roomId, userId);

            if(code < 0)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(code, "User isn't in this room").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

            // Record this action.
            chatService.leaveRoom(roomId, userId);

            // Form echo message.
            echoMsg += "LEFT_CHATROOM: " + roomId;
            echoMsg += "\nJOIN_ID: " + userId + "\n";

            broadcastFlag = true;

            broadcastMsg = "CHAT: " + roomId;
            broadcastMsg += "\nCLIENT_NAME: " + username;
            broadcastMsg += "\nMESSAGE: " + username +" has left this chatroom.\n\n";
        }
        // Handle "DISCONNECT" command.
        else if(msg.startsWith("DISCONNECT"))
        {
            // Form echo message.
            echoMsg = "GOODBYE!";

            // Return this message to caller and let it handle this command.
            returnMsg = "DISCONNECT";
        }
        // Handle "CHAT" command.
        else if(msg.startsWith("CHAT"))
        {
            String[] msgSeg = msg.split("\n");

            roomId = new Integer(msgSeg[0].split(" ")[1]);

            userId = new Integer(msgSeg[1].split(" ")[1]);

            // Due to blocking-IO of client side, this broadcast function cannot run as expected.
            // Thus just echo messge to sender.
//            chatService.broadcast(roomId, userId, msgSeg[3].split(" ")[1]);

            // Form echo message.
//            echoMsg += "CHAT: " + roomId;
//            echoMsg += "\nCLIENT_NAME: " + msgSeg[2].split(" ")[1];
//            echoMsg += "\nMESSAGE: " + msgSeg[3].split(" ")[1] + "\n";
            broadcastFlag = true;

            broadcastMsg = "CHAT: " + roomId;
            broadcastMsg += "\n" + msgSeg[2];
            broadcastMsg += "\n" + msgSeg[3] + "\n\n";

        }
        // Unexpected commands.
        else
        {
            echoMsg = "Unrecognized Command\n";
        }

        ByteBuffer echoByteBuffer = ByteBuffer.wrap(echoMsg.getBytes());

        if(!msg.startsWith("CHAT"))
        {
            channel.write(echoByteBuffer);
        }

        if(msg.startsWith("LEAVE_CHATROOM"))
        {
            echoByteBuffer = ByteBuffer.wrap(broadcastMsg.getBytes());

            channel.write(echoByteBuffer);
        }

        if(broadcastFlag){
            chatService.broadcast(roomId, userId, broadcastMsg);
        }

        return returnMsg;
    }

    public String formErrorMessage(int code, String desc)
    {
        return "ERROR_CODE: " + code + "\nERROR_DESCRIPRTION: " + desc +"\n";
    }

    public static void main(String[] args)
    {
        if(args.length < 1)
        {
            System.out.println("Usage: java classes/ChatServer <port>");

            System.exit(1);
        }

        try {
            ChatServer chatServer = new ChatServer(new Integer(args[0]));

            chatServer.loop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
