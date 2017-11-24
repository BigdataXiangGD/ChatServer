import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChatService {

    private Map<String, Integer> chatRoomIdMap;

    private Map<Integer, List<Integer>> userInRoom;

    private Map<String, Integer> userIdMap;

    private Map<Integer, SocketChannel> userChannels;

    private int nextChatRoomId;

    private int nextUserId;

    public ChatService()
    {
        chatRoomIdMap = new HashMap<>();

        userIdMap = new HashMap<>();

        userInRoom = new HashMap<>();

        userChannels = new HashMap<>();

        nextChatRoomId = 0;

        nextUserId = 0;
    }

    public int getUserId(String username)
    {
        if(!userIdMap.containsKey(username))
        {
            return -2;
        }

        return userIdMap.get(username);
    }

    public int getRoomId(String room)
    {
        if(chatRoomIdMap.containsKey(room))
        {
            return -3;
        }

        return userIdMap.get(room);
    }

    public int generateUserId(String username)
    {
		
        if(userIdMap.containsKey(username))
        {
            return -1;
        }

        userIdMap.put(username, nextUserId);

        ++nextUserId;

        return userIdMap.get(username);
    }

    public int joinRoom(String roomName, int userId, SocketChannel channel)
    {
		
        if(!chatRoomIdMap.containsKey(roomName))
        {
            chatRoomIdMap.put(roomName, nextChatRoomId);

            ++nextChatRoomId;

            userInRoom.put(chatRoomIdMap.get(roomName), new LinkedList<>());
        }

        userChannels.put(userId, channel);

        userInRoom.get(chatRoomIdMap.get(roomName)).add(userId);

        return chatRoomIdMap.get(roomName);
    }

    public int leaveRoom(int roomId, int userId)
    {
        List<Integer> userList = userInRoom.get(roomId);

        int idx = 0;

        for(Integer id : userList)
        {
            if(id.equals(userId))
            {
                break;
            }

            ++idx;
        }

        if(idx == userList.size())
        {
            return -4;
        }

        userList.remove(idx);

        String key = null;

        for(Map.Entry<String, Integer> entry : userIdMap.entrySet())
        {
            if(entry.getValue() == userId)
            {
                key = entry.getKey();

                break;
            }
        }

        userIdMap.remove(key);

        return 0;
    }

    public int broadcast(int roomId, int userId, String msg) throws IOException {

        msg = userId + ": " + msg;

        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());

        List<Integer> userIds = userInRoom.get(roomId);

        for(Integer id : userIds)
        {
            userChannels.get(id).write(byteBuffer);
        }

        return 0;

    }
}
