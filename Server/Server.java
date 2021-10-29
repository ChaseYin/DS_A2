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
import java.net.*;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.net.InetAddress.getLocalHost;

public class Server{

    //每个peer有一个自己的监听端口 输入"-p xxxx"来决定这个peer在哪个端口监听
    @Option(required = false, name = "-p", aliases = {"--port"}, usage = "port number")
    private static int port = 3000;

    @Option(required = false, name = "-h", aliases = {"--host"}, usage = "Host Address")
    private static String host;


//用来记录此peer连接到的remote peer的ip和监听端口
    protected static String connectedTo;

    //建立一个currentRoom
    protected static ChatRoom currentRoom;

    // count connected users
    protected static Integer guestCount = 0;

    //peer连接别人的时候的连接端口(仅仅是连接端口)
    protected static String peerConId;

    protected static PriorityQueue<Integer> nextLowestQueue = new PriorityQueue<>();

    // Connected user info    (  Uses Thread-Safe Variant of ArrayList   )
    protected static CopyOnWriteArrayList<String> userIdentities = new CopyOnWriteArrayList();

    //如果报"不安全的操作，则是这里"

    protected static ConcurrentLinkedQueue<String> networkList = new ConcurrentLinkedQueue<String>();

    protected static ConcurrentLinkedQueue<String> shoutList = new ConcurrentLinkedQueue<String>();

    protected static ConcurrentLinkedQueue<String> neighborList = new ConcurrentLinkedQueue<String>();

    protected static ConcurrentLinkedQueue<String> migrateList = new ConcurrentLinkedQueue<String>();

    //protected static CopyOnWriteArrayList<String> networkList = new CopyOnWriteArrayList();

    protected static CopyOnWriteArrayList<String> userKicked = new CopyOnWriteArrayList();

    protected static CopyOnWriteArrayList<ClientConnection> userThreads = new CopyOnWriteArrayList<>();

    protected static CopyOnWriteArrayList<ChatRoom> rooms = new CopyOnWriteArrayList<>();


    //private static int connect = 9999;//连接端口
    public static String identity;

    public static String listenIdentity;


    public static void main(String[] args) throws IOException, InterruptedException {
        new Server().doMain(args);
//        System.out.println("port2 是:"+connect);

        String ipAddress = getLocalHost().toString();
        //System.out.println("address1是："+ipAddress);
        String[] arr = ipAddress.split("/");
//        System.out.println("第一个参数是："+arr[0]);
//        System.out.println("第二个参数是："+arr[1]);
        String ip = arr[1];

        identity = ip+" listen: "+port;
        //System.out.println("当前peer的id为："+ip);


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

            Thread listeningThread = new Thread(new peerListenThread(ip,port));
            listeningThread.start();

            //DataOutputStream out = listeningThread.getOutput();


            //init the mainHall
//            ChatRoom mainHall = new ChatRoom("MainHall");
//            rooms.add(mainHall);


            while (true) {

                //如果有别的peer连接此peer的时候
                Socket socket = serverSocket.accept();//accept the requests
                System.out.println("New peer Connected...");


                //SocketAddress clientAddress=socket.getRemoteSocketPort();
                SocketAddress clientPort=socket.getRemoteSocketAddress();
                System.out.println("remote socket address是："+clientPort);
                String cc = clientPort + "";
                String[] res = cc.split(":");

                String[] kickCheck = cc.split("/");
                String commonId = kickCheck[1];

                peerConId = res[1];
                System.out.println("peerConId是："+peerConId);

                int guestId = getNextAvailableId();

                if (guestId == guestCount) {
                    increaseGuestCount();
                    guestId = guestCount;
                }

                //这个后面需要改成IP+port
                //String newGuest = "guest" + guestId;

                //userIdentities.add(newGuest);

                if(!userKicked.contains(commonId))
                {
                    ClientConnection client = new ClientConnection(socket, cc);

                    userThreads.add(client);
                    client.start();
                }else{
                    ClientConnection clientKick = new ClientConnection(socket, cc);
                    clientKick.kickNotice();
                    clientKick.quitRequest();
                }


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
            InetAddress address = getLocalHost();

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

    public static void migrate(String userId, String futureIp, String futurePort, String roomName){
        System.out.println("调用sever端的migrate（）方法");
        //userKicked.add(userId);

        //先发送需要重新connect的peer和重新进入的room
        //connection.migrateRequest（migratePeer,roomName）
        //让peerListenThread那边先把要加入的peer和room记录下来
        //然后再退出

        for (ClientConnection connection : userThreads) {
            if(userId.equals(connection.getIdentity()))
            {
                System.out.println("找到了需要migrate的用户");
                System.out.println("需要migrate的room为："+roomName);
                try{
                    //connection.createLocalRoomRequest(roomName,identity);

                    connection.migrateRequest(futureIp,futurePort,roomName);
                    //connection.quitRequest();
                }catch (IOException e){
                    System.out.println("踢出用户时捕获exception");
                }

            }
            else{
                System.out.println("找不到需要migrate的用户");
            }
            System.out.println("现在从server结束掉connection");
            connection.interrupt();


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

    public static void shout(String message) throws IOException {
        for (ChatRoom room : rooms) {
            System.out.println("需要shout的room有："+room.getRoomId());
            room.shoutToRoom(message);
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
        if(roomToBeDeleted!=null){

            rooms.remove(rooms.indexOf(roomToBeDeleted));
            try {
                for (int i = 0; i < roomToBeDeleted.getClientThreads().size(); i++) {
                    ClientConnection cc = roomToBeDeleted.getClientThreads().get(i);
                        cc.quitRequest();
                }


            }catch(Exception e){
                //System.out.println("该room中没有用户");
                e.printStackTrace();
            }
        }else{

            System.out.println("并不存在此room");
        }




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

    //后面需要写一下这个函数
    private boolean isAlreadyInRoom(String identity, String roomId) {
        ChatRoom room = Server.getRoom(roomId);
        if (room != null) {
            for (String user : room.getUsers()) {
                if (user.equals(identity)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void localJoinRoom(String roomId) throws IOException {

        System.out.println(identity+ " has joined "+roomId);
        //ChatRoom MainHall = Server.rooms.get(0);
        //currentRoom = MainHall;

        ChatRoom roomJoin = Server.getRoom(roomId);

        try {
            if (roomJoin != null) {

                //第一次加入room
                if (currentRoom == null) {
                    String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", roomId);
                    roomJoin.broadcastToRoom(roomChangeMessage);
                    currentRoom = roomJoin;
                    currentRoom.getUsers().add(identity);
                } else {
                    String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, currentRoom.getRoomId(), roomId);
                    // Broadcast changes to rooms
                    //String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, currentRoom.getRoomId(), roomId);

                    currentRoom.broadcastToRoom(roomChangeMessage);
                    roomJoin.broadcastToRoom(roomChangeMessage);

                    // Remove from current room
                    currentRoom.removeUser(identity);

                    // Delete previous room if they are the owner and no one is in it
                    //deleteRoomIfOwner(identity, currentRoom);

                    // Put user in the new room
                    currentRoom = roomJoin;

                    // Record them as now being in the new room
                    currentRoom.getUsers().add(identity);
                }


            } else {

                System.out.println("要加入的room为null！");

            }
        }catch(NullPointerException e){
            System.out.println("用户第一次加入room");
        }
    }


    public static String getRoomMem(String roomId) throws IOException {
        if (roomId.equals("MainHall")) {

            String[] users = Server.rooms.get(0).getUsers().toArray(new String[Server.rooms.get(0).getUsers().size()]);

            String whoMainHallResponse = new ServerMessage().roomContentsMsg("MainHall", "", users);
            return whoMainHallResponse;
//            getOutput().writeUTF(whoMainHallResponse);
//            getOutput().flush();

        } else {
            ChatRoom roomWho = Server.getRoom(roomId);
            if (roomWho != null) {
                String roomOwner = roomWho.getOwner();
                String[] usersInside = roomWho.getUsers().toArray(new String[Server.getRoom(roomId).getUsers().size()]);

                String whoResponse = new ServerMessage().roomContentsMsg(roomId, roomOwner, usersInside);
                return whoResponse;
//                getOutput().writeUTF(whoResponse);
//                getOutput().flush();
            }
            else{
                //System.out.println("请求的room不存在！");
                        return "请求的room不存在！";
            }
        }
    }



}
