package Server;

import Client.SendThread;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server{

    //每个peer有一个自己的监听端口 输入"-p xxxx"来决定这个peer在哪个端口监听
    @Option(required = false, name = "-p", aliases = {"--port"}, usage = "port number")
    private static int port = 3000;

    @Option(required = false, name = "-h", aliases = {"--host"}, usage = "Host Address")
    private static String host;


    // count connected users
    protected static Integer guestCount = 0;
    protected static PriorityQueue<Integer> nextLowestQueue = new PriorityQueue<>();

    // Connected user info    (  Uses Thread-Safe Variant of ArrayList   )
    protected static CopyOnWriteArrayList<String> userIdentities = new CopyOnWriteArrayList();
    protected static CopyOnWriteArrayList<String> userKicked = new CopyOnWriteArrayList();

    protected static CopyOnWriteArrayList<ClientConnection> userThreads = new CopyOnWriteArrayList<>();

    protected static CopyOnWriteArrayList<ChatRoom> rooms = new CopyOnWriteArrayList<>();


    //private static int connect = 9999;//连接端口
    private static String identity;

    public static void main(String[] args) throws IOException, InterruptedException {
        new Server().doMain(args);
//        System.out.println("port2 是:"+connect);

        String ipAddress = InetAddress.getLocalHost().toString();
        System.out.println("address1是："+ipAddress);


//        String serverIP = getServerIp();
//        System.out.println("address2是："+getServerIp());



        ServerSocket serverSocket = null;
        try {
            //初始化server的socket
            //首先给peer分配一个端口去监听请求（-p 3000 or -p xxxx）
            //此peer在port端口号监听
            serverSocket = new ServerSocket(port);
//            System.out.println("Server is listening on port " + port + "...");
            System.out.println("This peer is currently listening on port " + port + "...");

            Thread listeningThread = new Thread(new peerListenThread(ipAddress,port));
            listeningThread.start();


            //init the mainHall
            ChatRoom mainHall = new ChatRoom("MainHall");
            rooms.add(mainHall);


            while (true) {

                //如果有别的peer连接此peer的时候
                Socket socket = serverSocket.accept();//accept the requests
                System.out.println("New peer Connected...");

                int guestId = getNextAvailableId();

                if (guestId == guestCount) {
                    increaseGuestCount();
                    guestId = guestCount;
                }

                //这个后面需要改成IP+port
                String newGuest = "guest" + guestId;

                //userIdentities.add(newGuest);

                ClientConnection client = new ClientConnection(socket, newGuest);

                userThreads.add(client);
                client.start();
                //建立连接peer的connection
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
                closeAllThreads();
            }
        }
    }




    /**
     * 获取当前服务器ip地址
     */
    private static String getServerIp() {
        try {
            //用 getLocalHost() 方法创建的InetAddress的对象
            InetAddress address = InetAddress.getLocalHost();

            return address.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    public static void addKickUsers(String userId){

        userKicked.add(userId);
        //kickUserThread
        for (ClientConnection connection : userThreads) {
            if(userId.equals(connection.getIdentity()))
            {
                System.out.println("The user has been found");
                try{
                    connection.quitRequest();
                }catch (IOException e){
                    System.out.println("踢出用户时捕获exception");
                }

            }
            else{
                System.out.println("The user has been not found");
            }

        }
    }



    private static void closeAllThreads() throws InterruptedException {
        for (ClientConnection connection : userThreads) {
            connection.join();
        }
    }

    public static void announce(String message) throws IOException {
        for (ClientConnection connection : userThreads) {
            DataOutputStream out = connection.getOutput();

            Thread messageSender = new Thread(new ServerSendThread(out, message));
            messageSender.start();
        }
    }

    public static ChatRoom getRoom(String roomId) {
        for (ChatRoom room : rooms) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

//    public static void createRoom(String roomId, String owner) {
    public static void createRoom(String roomId, String owner) {
        synchronized (rooms) {
            //lock this object until current operation is finished

            ChatRoom newRoom = new ChatRoom(roomId);
            newRoom.setOwner(owner);
            rooms.add(newRoom);
            System.out.println("The room called: "+roomId+" has been successfully created");


        }
    }
    public static void createLocalRoom(String newRoomId, String owner) throws IOException {
        boolean roomNameInUse = false;
        for (ChatRoom rm : Server.rooms) {
            if (rm.getRoomId().equals(newRoomId)) {
                //newRoom has already been used
                roomNameInUse = true;
            }
        }

        if (roomNameInUse) {

            ArrayList<JSONObject> roomsResponse = getRoomListWithCount();
            //return information of the room
            String roomListResponse = new ServerMessage().roomListMsg(roomsResponse);
            System.out.println("The room called: "+newRoomId+" has been used already");

        } else {
            //  create new room
            //成功创建rooms
            createRoom(newRoomId, owner);
            //System.out.println("成功创建room:"+newRoomId);

            ArrayList<JSONObject> roomsResponse = getRoomListWithCount();
            String roomListResponse = new ServerMessage().roomListMsg(roomsResponse);

        }
    }

    public static void deleteRoom(String roomId) {

        ChatRoom roomToBeDeleted = getRoom(roomId);
        rooms.remove(rooms.indexOf(roomToBeDeleted));
        //Firstly create a copy, process the operation in this new copy and then re-direct
        //the original address to this one
    }
    public static ArrayList<JSONObject> getRoomListWithCount() {

        ArrayList<JSONObject> roomsWithCount = new ArrayList<>();

        for (ChatRoom room : Server.rooms) {

            JSONObject roomCount = new JSONObject();

            roomCount.put("roomid", room.getRoomId());
            roomCount.put("count", room.getUsers().size());

            roomsWithCount.add(roomCount);

        }
        return roomsWithCount;
    }



    public static void makeIdAvailable(Integer id) {
        synchronized (nextLowestQueue) {
            nextLowestQueue.add(id);
        }
    }

    public static int getNextAvailableId() {
        synchronized (nextLowestQueue) {
            if (!nextLowestQueue.isEmpty()) {
                int nextLowest = nextLowestQueue.poll();
                increaseGuestCount();
                return nextLowest;
            }
            return guestCount;
        }
    }

    public static ClientConnection getUserThread(String userId) {
        synchronized (userThreads) {
            for (ClientConnection clientConnection : userThreads) {
                if (clientConnection.getIdentity().equals(userId)) {
                    return clientConnection;
                }
            }
        }
        return null;
    }

    public static void increaseGuestCount() {
        synchronized (guestCount) {
            guestCount++;
        }
    }

    public static void decreaseGuestCount() {
        synchronized (guestCount) {
            guestCount--;
        }
    }

    public void doMain(String[] args) throws IOException {

        CmdLineParser parser = new CmdLineParser(this);
        try{
            parser.parseArgument(args);
//            if (arguments.isEmpty())
//            throw new CmdLineException("No argument is given");
        } catch (CmdLineException e){
            System.err.println(e.getMessage());
        }
    }





    //原来client的方法
    private static void messageReply(JSONObject jsonMsg) {
        //display clients' chat content

        System.out.print(jsonMsg.get("identity").toString() + ": " + jsonMsg.get("content").toString());
        System.out.println();

    }

    private static void newIdentityReply(JSONObject jsonMsg) {
        // first join the chatroom
        if (identity == null) {
            identity = jsonMsg.get("identity").toString();
            System.out.println("Connected to localhost as " + identity);
        } else if (jsonMsg.get("former").toString().equals(jsonMsg.get("identity"))) {
            System.out.println("Requested identity invalid or in use");
        } else {
            identity = jsonMsg.get("identity").toString();
            System.out.println("notice that " +jsonMsg.get("former").toString() + " is now " + identity);
        }
    }

    private static void roomListReply(JSONObject jsonMsg) {
        JSONArray roomList = (JSONArray) jsonMsg.get("rooms");
        for (int i = 0; i < roomList.size(); i++) {
            //print the room id and count of each room
            JSONObject singleRoom = (JSONObject) roomList.get(i);
            System.out.print(singleRoom.get("roomid").toString() + ": ");
            System.out.print(singleRoom.get("count").toString() + " guest/s");
            System.out.println();

        }
    }

    private static void roomContentsReply(JSONObject jsonMsg) {

        String currentRoom = jsonMsg.get("roomid").toString();
        //get all identities of the room
        JSONArray jsonRoomMembers = (JSONArray) jsonMsg.get("identities");

        ArrayList<String> roomMembers = new ArrayList<>();

        for (int i = 0; i < jsonRoomMembers.size(); i++) {
            //use msg from server to add user into rooms
            roomMembers.add(jsonRoomMembers.get(i).toString());

        }

        if (currentRoom.equals("MainHall")) {
            System.out.print(currentRoom + " contains ");

            for (String member : roomMembers) {
                System.out.print(member + " ");
            }
            System.out.println();
        }
        else {

            System.out.print(currentRoom + " contains ");
            for (String member : roomMembers) {
                System.out.print(member + " ");
            }
            System.out.println();
            System.out.print("The owner is: " + jsonMsg.get("owner").toString());
            System.out.println();
        }

    }

}
