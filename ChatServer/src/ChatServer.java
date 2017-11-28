import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

public class ChatServer {
	
    // A selector let program know which socket is active.
    private Selector selector;
    //converting messages
    private ByteBuffer byteBuffer;

    private Charset charset;

    private CharsetDecoder decoder;

    private String serverIP;

    private String serverPort;
	
    // Use ChatService to manage users.
    private ChatService chatService;

    public ChatServer(int port) throws IOException {

        byteBuffer = ByteBuffer.allocate(2048);

        charset = Charset.forName("UTF-8");

        decoder = charset.newDecoder();

	// Create server socket.
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        selector = Selector.open();

        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        serverIP = serverSocketChannel.socket().getInetAddress().toString().substring(1);

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

                processedInfo = process(key);

                if(processedInfo.equals("KILL_SERVICE"))
                {
                    stopFlag = true;

                    break;
                }

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

          
            if(key.isAcceptable())
            {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                serverSocketChannel.close();
            }
           
            else
            {
                SocketChannel socketChannel = (SocketChannel) key.channel();

                socketChannel.close();
            }
        }

        
        selector.close();
    }

    // Process command from client.
    public String process(SelectionKey key) throws IOException {

        byteBuffer.clear();

      
        if(key.isAcceptable())
        {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

            SocketChannel channel = serverSocketChannel.accept();
			
            channel.configureBlocking(false);

            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        
        else if(key.isReadable())
        {
            SocketChannel channel = (SocketChannel) key.channel();

            int count = channel.read(byteBuffer);

            if(count > 0)
            {
                byteBuffer.flip();

                String msg = decoder.decode(byteBuffer).toString().trim();

                byteBuffer.clear();

                if(msg.equals("KILL_SERVICE"))
                {
                    return msg;
                }
                else
                {
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

    public String handleMessage(SocketChannel channel, String msg) throws IOException {

        String echoMsg = "", returnMsg = "";

        if(msg.equals("HELO BASE_TEST"))
        {
            echoMsg += "HELO BASE_TEST\nIP:";
            echoMsg += channel.socket().getInetAddress().toString().substring(1);
            echoMsg += "\nPort:";
            echoMsg += channel.socket().getPort();
            echoMsg += "\nStudentID:17301984\n";
        }

        else if(msg.startsWith("JOIN_CHATROOM"))
        {
            String[] msgSeg = msg.split("\n");

            int userid = chatService.generateUserId(msgSeg[3].split(" ")[1]);

            if(userid == -1)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(-1, "Username existed").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

         
            chatService.joinRoom(msgSeg[0].split(" ")[1], userid, channel);

            echoMsg += "JOINED_CHATROOM: " + msgSeg[0].split(" ")[1];
            echoMsg += "\nSERVER_IP: " + serverIP;
            echoMsg += "\nPORT: " + serverPort;
            echoMsg += "\nROOM_REF: " + 0;
            echoMsg += "\nJOIN_ID: " + userid + "\n";
        }

        else if(msg.startsWith("LEAVE_CHATROOM"))
        {
            String[] msgSeg = msg.split("\n");

            int userid = chatService.getUserId(msgSeg[2].split(" ")[1]);

            if(userid < 0)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(userid, "User doesn't exit").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

            int roomId = new Integer(msgSeg[0].split(" ")[1]);

            int code = chatService.leaveRoom(roomId, userid);

            if(code < 0)
            {
                ByteBuffer echoByteBuffer = ByteBuffer.wrap(
                        formErrorMessage(code, "User isn't in this room").getBytes()
                );

                channel.write(echoByteBuffer);

                return returnMsg;
            }

            chatService.leaveRoom(roomId, userid);

            echoMsg += "LEFT_CHATROOM: " + roomId;
            echoMsg += "\nJOIN_ID: " + userid + "\n";
        }
		
        else if(msg.startsWith("DISCONNECT"))
        {
            echoMsg = "GOODBYE!";

            returnMsg = "DISCONNECT";
        }

        else if(msg.startsWith("CHAT"))
        {
            String[] msgSeg = msg.split("\n");

            int roomId = new Integer(msgSeg[0].split(" ")[1]);

            int userId = new Integer(msgSeg[1].split(" ")[1]);

            echoMsg += "CHAT: " + roomId;
            echoMsg += "\nCLIENT_NAME: " + msgSeg[2].split(" ")[1];
            echoMsg += "\nMESSAGE: " + msgSeg[3].split(" ")[1] + "\n";

        }

        else
        {
            echoMsg = "Unrecognized Command\n";
        }

        ByteBuffer echoByteBuffer = ByteBuffer.wrap(echoMsg.getBytes());

        channel.write(echoByteBuffer);

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
