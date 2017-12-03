import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A Class for managing chat rooms.
 * Assign correct id from both users and rooms.
 * Map user id from username.
 * Map room id from room name.
 * Record users in which room.
 * Provide broadcast a message to all user in same room.
 */
public class ChatService {

    // For getting room id from room name.
    private Map<String, Integer> chatRoomIdMap;

    // Record which user in which room.
    private Map<Integer, List<Integer>> userInRoom;

    // For getting user id from username.
    private Map<String, Integer> userIdMap;

    // For getting corresponding socket from user id.
    private Map<Integer, SocketChannel> userChannels;

    private int nextChatRoomId;

    private int nextUserId;

    public ChatService()
    {
        // Initialize all data objects.
        chatRoomIdMap = new HashMap<>();

        userIdMap = new HashMap<>();

        userInRoom = new HashMap<>();

        userChannels = new HashMap<>();

        nextChatRoomId = 0;

        nextUserId = 0;
    }

    // Get user id from username.
    public int getUserId(String username)
    {
        if(!userIdMap.containsKey(username))
        {
            return -2;
        }

        return userIdMap.get(username);
    }

    // Get room id from room name.
    public int getRoomId(String room)
    {
        if(chatRoomIdMap.containsKey(room))
        {
            return -3;
        }

        return userIdMap.get(room);
    }

    // If a new user join in the chat room, firstly gets an ID.
    public int generateUserId(String username)
    {
        // If a same username has existed.
        if(!userIdMap.containsKey(username))
        {
            userIdMap.put(username, nextUserId);

            ++nextUserId;
        }

        return userIdMap.get(username);
    }

    // Join a room.
    public int joinRoom(String roomName, int userId, SocketChannel channel)
    {
        // If the room doesn't exist, then create new one.
        if(!chatRoomIdMap.containsKey(roomName))
        {
            chatRoomIdMap.put(roomName, nextChatRoomId);

            ++nextChatRoomId;

            userInRoom.put(chatRoomIdMap.get(roomName), new LinkedList<>());
        }

        // Record the socket.
        userChannels.put(userId, channel);

        // Add user to corresponding room.
        userInRoom.get(chatRoomIdMap.get(roomName)).add(userId);

        return chatRoomIdMap.get(roomName);
    }

    // A user leaves room.
    public int leaveRoom(int roomId, int userId)
    {
        List<Integer> userList = userInRoom.get(roomId);

        int idx = 0;

        // Find out the user.
        for(Integer id : userList)
        {
            if(id.equals(userId))
            {
                break;
            }

            ++idx;
        }

        // User doesn't stay in this room.
        if(idx == userList.size())
        {
            // Return error code.
            return -4;
        }

        // Remove this user from this room.
        userList.remove(idx);

        String key = null;

        // Remove this user's socket.
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

    // Broadcast a message to all user in same room.
    public int broadcast(int roomId, int userId, String msg) throws IOException {

//        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());

        List<Integer> userIds = userInRoom.get(roomId);

        // Send to all user in this room.
        for(Integer id : userIds)
        {
            ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
            userChannels.get(id).write(byteBuffer);
        }

        return 0;

    }
}
