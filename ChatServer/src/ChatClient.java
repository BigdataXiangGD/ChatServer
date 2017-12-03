import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class ChatClient {

    private SocketChannel socketChannel;

    // Initialize socket.
    public ChatClient(String host, int port) throws IOException
    {
        socketChannel = SocketChannel.open();

        socketChannel.connect(new InetSocketAddress(host, port));
    }

    // Wrapper function for sending message.
    public boolean send(String msg) throws IOException
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());

        socketChannel.write(byteBuffer);

        return true;
    }

    // Wrapper function for receiving message.
    public String receive() throws IOException
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        socketChannel.read(byteBuffer);

        byteBuffer.flip();

        Charset charset = Charset.forName("UTF-8");

        CharsetDecoder charsetDecoder = charset.newDecoder();

        return charsetDecoder.decode(byteBuffer).toString().trim();
    }

    public void close() throws IOException
    {
        socketChannel.close();
    }

    public static void main(String[] args) throws IOException
    {
        if(args.length < 2)
        {
            System.out.println("Usage: java classes/ChatClient <server address> <server port>");

            System.exit(1);
        }

        ChatClient chatClient = new ChatClient(args[0], new Integer(args[1]));

        // Send "HELO text" and print the returned message.
        chatClient.send("HELO text");

        String echo = chatClient.receive();

        System.out.println(echo);

        // Send "JOIN_CHATROOM" and print the returned message.
        String msg = "JOIN_CHATROOM: SWITCH\nCLIENT_IP: 0\nPORT: 0\nCLIENT_NAME: TEST1";

        chatClient.send(msg);

        echo = chatClient.receive();

        String[] details = echo.split("\n");

        int userId = new Integer(details[4].split(" ")[1]);

        int roomId = new Integer(details[3].split(" ")[1]);

        System.out.println(echo);

        // Send "CHAT" and print the returned message.
        msg = "CHAT: " + roomId + "\nJOIN_ID: " + userId + "\nCLIENT_NAME: TEST1\n"
                + "MESSAGE: Hello everyone!";

        chatClient.send(msg);

        echo = chatClient.receive();

        System.out.println(echo);

        // Send "LEAVE_CHATROOM" and print the returned message.
        msg = "LEAVE_CHATROOM: " + roomId + "\nJOIN_ID:" + userId + "\nCLIENT_NAME: TEST1";

        chatClient.send(msg);

        echo = chatClient.receive();

        System.out.println(echo);

        // Send "DISCONNECT" and print the returned message.
        msg = "DISCONNECT: 0\nPORT: 0\nCLIENT_NAME: TEST1";

        chatClient.send(msg);

        echo = chatClient.receive();

        System.out.println(echo);

        // Then server will close this client's socket.
        // The closing action shouldn't be taken in client side.
        // chatClient.close();
    }
}
