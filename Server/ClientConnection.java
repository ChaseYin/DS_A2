package Server;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static Server.Server.*;


public class ClientConnection extends Thread {

    String identity;
    String listenPort;//
    String listenIdentity;


    protected Socket socket;
    protected ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<String>();
    //could be safely accessed by mutiple threads
    protected ChatRoom currentRoom;
    //protected ConcurrentHashMap<ClientConnection, Ban> bannedUsers;


    public ClientConnection(Socket socket, String identity) {
        //constructor
        this.identity = identity;
        this.socket = socket;

    }

    public DataOutputStream getOutput() throws IOException {

        return new DataOutputStream(socket.getOutputStream());

    }


    public String getIdentity() {

        return identity;

    }

    public String getListenIdentity() {

        return listenIdentity;

    }



    //  receive messages from clients
    @Override
    public void run() {

        try {

            SocketAddress clientPort=socket.getRemoteSocketAddress();

            //System.out.println("连接的peer的连接id是："+clientPort);

            //get inputStream and outputStream
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            //read message from messageQueue
            Thread readFromQueue = new Thread(new ReadWrite());
            readFromQueue.start();

            try {
                while (true) {
                    //protect the resource
                    synchronized (messageQueue) {
                        String message = in.readUTF();
                        System.out.println(message);
                        messageQueue.add(message);//push clients' message into messageQueue
                    }
                }
            } catch (SocketException | EOFException e) {
                //quitRequest();
                in.close();
                out.close();
                socket.close();
                System.out.println("Client " + identity + " terminated connection");
            }
        } catch (IOException e) {
            System.out.println("IO exception happened!");
            //e.printStackTrace();
        }

    }


    //  respond the messages that stored in messageQueue
    private class ReadWrite implements Runnable {

        @Override
        public void run() {


            boolean readMessages = true;

            while (readMessages) {

                while (!messageQueue.isEmpty() && readMessages) {
                    //messageQueue is not empty
                    //avoid throws exception

                    //获得messageQueue里的message
                    String message = messageQueue.poll();
                    Object obj = JSONValue.parse(message);
                    JSONObject jsonMsg = (JSONObject) obj;

                    String type = jsonMsg.get("type").toString();

                    try {

                        switch (type) {
                            case "message":
                                jsonMsg.put("identity", identity);
                                //这里因为刚连接就发送消息会报错
                                if(currentRoom == null)
                                {
                                    //System.out.println("当前还没有加入room，不能发送消息");
                                    System.out.println("This peer has not joined any rooms yet");
                                }
                                else{
                                currentRoom.broadcastToRoom(jsonMsg.toString());}
                                break;

                            case "migrate":
                                //System.out.println("Server端收到migrate请求，现在开始创建room");
                                String migrateRoom = jsonMsg.get("migrateRoom").toString();
                                Server.createLocalRoom(migrateRoom+"(migrated)", Server.identity);
                                clientJoinRequest(migrateRoom+"(migrated)");
                                break;

                            case "shout":

                                //先给parent发送shout请求
                                try{
                                    //获得当前peer的上层peer
                                    String[] splitArr = Server.connectedTo.split(":");
                                    Socket sendupSocket = null;
                                    sendupSocket = new Socket(splitArr[0], Integer.parseInt(splitArr[1]));

                                    DataInputStream ins = new DataInputStream(sendupSocket.getInputStream());

                                    Thread sendUp = new Thread(new sendUpThread(sendupSocket,jsonMsg.get("id").toString(),jsonMsg.get("content").toString()));
                                    sendUp.start();
                                    //System.out.println("当前peer为："+element);
                                }catch(NullPointerException e){
                                    System.out.println("");
                                    //System.out.println("当前peer已经是最顶部peer");
                                    //e.printStackTrace();
                                }

                                //System.out.println("收到shoutMSG");
                                Server.shout(jsonMsg.toString());


                                //currentRoom.broadcastToRoom(jsonMsg.toString());
                                String shouter = jsonMsg.get("id").toString();
                                String msg = jsonMsg.get("content").toString();

//                                outputStream.writeUTF(shoutMsg);
//                                outputStream.flush();
//                                System.out.println(shouter+" shouted: "+msg);

                                break;


                            case "join":
                                String roomId = jsonMsg.get("roomid").toString();
                                clientJoinRequest(roomId);
                                break;

                            case "list":
                                ArrayList<JSONObject> roomsCount = getRoomListWithCount();
                                String response = new ServerMessage().roomListMsg(roomsCount);
                                getOutput().writeUTF(response);
                                getOutput().flush();
                                break;

                            case "listneighbors":
                                getNeighbors();
//                                String response = new ServerMessage().neighborMsg(neighbors);
//                                getOutput().writeUTF(response);
//                                getOutput().flush();
                                break;

                            case "createroom":
                                String newRoomId = jsonMsg.get("roomid").toString();
                                createRoomRequest(newRoomId);
                                break;

                            case "delete":
                                String roomToDelete = jsonMsg.get("roomid").toString();
                                deleteRoomRequest(roomToDelete);
                                break;

                            case "who":
                                String whoRequested = jsonMsg.get("roomid").toString();
                                whoRequest(whoRequested);
                                break;

                            case "identitychange":
                                String newIdentityRequest = jsonMsg.get("identity").toString();
                                String previousIdentity = identity;
                                identityChangeRequest(newIdentityRequest, previousIdentity);
                                break;

                            case "hostchange":
                                String conIdentity = jsonMsg.get("identity").toString();
                                String lisIdentity = jsonMsg.get("listenIdentity").toString();
                                storeIdentity(conIdentity,lisIdentity);
                                firstConnectionRequest();
                                checkKicked(conIdentity);
                                break;


                            case "quit":
                                quitRequest();
                                readMessages = false;
                                break;

                            default:
                                System.out.println("Error happened when reading clients' message");
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Error happened when receiving client message");
                        e.printStackTrace();
                    }
                }
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error happened when closing socket on disconnection");
                e.printStackTrace();
            }
        }

    }
    private void firstConnectionRequest() throws IOException {

        String roomChangeMsg = new ServerMessage().roomChangeMsg(getIdentity(), "", "");
        getOutput().writeUTF(roomChangeMsg);
        getOutput().flush();

    }

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

    private boolean isUsernameInUse(String newUsername) {
        for (int i = 0; i < Server.userIdentities.size(); i++) {
            if (Server.userIdentities.get(i).contains(newUsername)) {
                //in use
                return true;
            }
        }
        return false;
    }


    private ArrayList<JSONObject> getRoomListWithCount() {

        ArrayList<JSONObject> roomsWithCount = new ArrayList<>();

        for (ChatRoom room : Server.rooms) {

            JSONObject roomCount = new JSONObject();

            roomCount.put("roomid", room.getRoomId());
            roomCount.put("count", room.getUsers().size());

            roomsWithCount.add(roomCount);

        }
        return roomsWithCount;
    }

    private void getNeighbors(){

        try {
            //现在把userIdentities替换成networkList试一下
            //String[] users = Server.userIdentities.toArray(new String[Server.userIdentities.size()]);

            //把后面修改成了neighborlist，原来是networklist
            String[] users = neighborList.toArray(new String[neighborList.size()]);

            if(users.length==0)
            {
                //System.out.println("现在此peer还没有neighbor");
                System.out.println("This peer does not have neighbors right now");
            }else{
                //System.out.println("获得的已连接用户是：" + users[0]);
                String neighborResponse = new ServerMessage().neighborMsg(users);
                getOutput().writeUTF(neighborResponse);
                getOutput().flush();
            }

        }catch(IOException e){
            e.printStackTrace();
        }

    }

    private void removeAnyRoomOwnerships(String identity) {
        for (ChatRoom room : Server.rooms) {
            if (room.getOwner() != null) {
                if (room.getOwner().equals(identity)) {
                    room.setOwner("");
                    if (room.getUsers().isEmpty()) {
                        // if room is empty, then delete this room
                        deleteRoom(room.getRoomId());
                    }
                }
            }
        }
    }


    private void deleteRoom(String roomId) {
        ChatRoom chatRoom = Server.getRoom(roomId);
        Server.rooms.remove(Server.rooms.indexOf(chatRoom));
    }


    private void removeClientFromPreviousRoom(String roomId, String identity) {
        //System.out.println("现在要处理移除client的room是："+roomId);
        //System.out.println("要移除的client是："+identity);

//        if (roomId != null && !roomId.equals("MainHall")) {
        if (roomId != null) {
            ChatRoom previousRoom = Server.getRoom(roomId);
            previousRoom.removeUser(identity);
        }
    }

    private void moveAllToMainHall(String roomId) throws IOException {

        ChatRoom chatRoom = Server.getRoom(roomId);
        ChatRoom mainHall = Server.rooms.get(0);

//        for(int i = 0; i < chatRoom.getClientThreads().size(); i++ )/////
//        {
//            mainHall.addClient(chatRoom.getClientThreads().get(i));
//
//        }


        mainHall.addClient(ClientConnection.this);
        mainHall.getUsers().add(identity);

        for (String user : chatRoom.getUsers()) {

            String roomChangeMsg = new ServerMessage().roomChangeMsg(user, chatRoom.getRoomId(), "MainHall");

            chatRoom.broadcastToRoom(roomChangeMsg);
            mainHall.broadcastToRoom(roomChangeMsg);
        }
    }

    private void deleteRoomIfOwner(String leavingUser, ChatRoom leavingRoom) {
        if (leavingRoom.getOwner() != null) {
            if (leavingRoom.getOwner().equals(leavingUser)) {
                if (leavingRoom.getUsers().size() == 0) {
                    Server.deleteRoom(leavingRoom.getRoomId());
                }
            }
        }
    }


    private void clientJoinRequest(String roomId) throws IOException {
        //这里后面需要加一个判断传来的room是否存在

        //要加入的room是MainHall
//        if (roomId.equals("MainHall")) {
//            if (!isAlreadyInRoom(identity, roomId)) {
//
//                if (currentRoom != null) {
//                    //System.out.println("当前所在room是："+currentRoom.getRoomId());
//                    removeClientFromPreviousRoom(currentRoom.getRoomId(), identity);
//
//                }
//                // Add them to the room
//                ChatRoom mainHall = Server.rooms.get(0);
//                currentRoom = mainHall;
//                currentRoom.getClientThreads().add(ClientConnection.this);        // Add new thread to chatroom(mainHall)
//
//                //System.out.println("当前peer的id是："+ClientConnection.this.getIdentity());
//
//                currentRoom.getUsers().add(ClientConnection.this.getIdentity()); // Add username to list of mainHall
//
////               for(ClientConnection c: currentRoom.getClientThreads())
////               {
////                   System.out.println("MainHall中的threads包含："+c.getIdentity());
////               }
//
//                // Send room change message to all in the room
//                String response = new ServerMessage().roomChangeMsg
//                        (identity, "", "MainHall");
//                Server.rooms.get(0).broadcastToRoom(response);
//
//                // List the people in Main Hall to the single user
//                String[] mainHallClients = mainHall.getUsersArray();
//                String listMessage = new ServerMessage().roomContentsMsg("MainHall", "", mainHallClients);
//                getOutput().writeUTF(listMessage);
//                getOutput().flush();
//            }else{
//                //System.out.println("当前用户已经在room中了哦");
//                System.out.println("The user is already in the room");
//            }
//
//        }

            //要加入的room不是MainHall
            ChatRoom joinRoom = Server.getRoom(roomId);

            if (currentRoom != null) {
                removeClientFromPreviousRoom(currentRoom.getRoomId(), identity);

            }


            if(joinRoom!=null){
                if(currentRoom!=null){
                    String response = new ServerMessage().roomChangeMsg(identity, "", roomId);
                    currentRoom.broadcastToRoom(response);
                }
                String response = new ServerMessage().roomChangeMsg(identity, "", roomId);
                joinRoom.broadcastToRoom(response);


                currentRoom = joinRoom;
                //System.out.println("要加入的room："+roomId+"存在");
                //joinRoom.getClientThreads().add(ClientConnection.this);
                joinRoom.addClient(ClientConnection.this);
                joinRoom.getUsers().add(ClientConnection.this.getIdentity());

//                for(ClientConnection c: joinRoom.getClientThreads())
//                {
//                    System.out.println("要加入的room中的threads包含："+c.getIdentity());
//                }


                //joinRoom.broadcastToRoom(response);


                getOutput().writeUTF(response);
                getOutput().flush();

                ClientConnection thisConnection = ClientConnection.this;

                if (!isAlreadyInRoom(identity, roomId)) {

                    joinRoom(roomId);

                }

            }else{
                String MSG = new ServerMessage().roomChangeCheckMsg(roomId,"non-existent");
                //System.out.println("要加入的room："+roomId+"不存在");
                System.out.println("The room："+roomId+"is non-existent");
                getOutput().writeUTF(MSG);
                getOutput().flush();
            }



    }

    private void joinRoom(String roomId) throws IOException {
        //System.out.println("进入joinRoom方法");

        ChatRoom room = Server.getRoom(roomId);

        //需要加入的room存在
        if (room != null) {

            if(currentRoom == null){
                //System.out.println("当前room为空");
                // Broadcast changes to rooms
                String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", roomId);

                //currentRoom.broadcastToRoom(roomChangeMessage);
                room.broadcastToRoom(roomChangeMessage);

                // Put user in the new room
                currentRoom = room;

                // Record them as now being in the new room
                try {
                    currentRoom.getUsers().add(identity);
                    currentRoom.getClientThreads().add(ClientConnection.this);
                }catch(NullPointerException e){
                    //System.out.println("当前room还没有用户");
                    System.out.println("");
                }
            }else{
                //System.out.println("当前room不为空");
                // Broadcast changes to rooms
                String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, currentRoom.getRoomId(), roomId);

                currentRoom.broadcastToRoom(roomChangeMessage);
                room.broadcastToRoom(roomChangeMessage);

                // Remove from current room
                currentRoom.removeUser(identity);

                // Delete previous room if they are the owner and no one is in it
                //deleteRoomIfOwner(identity, currentRoom);

                // Put user in the new room
                currentRoom = room;

                // Record them as now being in the new room
                currentRoom.getUsers().add(identity);
                currentRoom.getClientThreads().add(ClientConnection.this);
            }


        }
        else{

            String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", roomId);
            //System.out.println("当前room不存在");
            System.out.println("The room is non-existent");
            // Put user in the new room
            currentRoom = room;

            // Record them as now being in the new room
            //currentRoom.getUsers().add(identity);
            //currentRoom.getClientThreads().add(ClientConnection.this);

        }
    }
    public void createLocalRoomRequest(String newRoomId, String owner) throws IOException {
      Server.createLocalRoom(newRoomId,owner);
    }

    public void createRoomRequest(String newRoomId) throws IOException {
        //System.out.println("开始请求创建新room！");

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
            getOutput().writeUTF(roomListResponse);
            getOutput().flush();

        } else {
            //  create new room
            //成功创建rooms
            Server.createRoom(newRoomId, identity);
            identityChangeRequest(identity+'*', identity);

            //System.out.println("成功创建room:"+newRoomId);
            System.out.println("Create room:"+newRoomId);

            ArrayList<JSONObject> roomsResponse = getRoomListWithCount();
            String roomListResponse = new ServerMessage().roomListMsg(roomsResponse);

            getOutput().writeUTF(roomListResponse);
            getOutput().flush();
        }
    }

    private void deleteRoomRequest(String roomToDelete) throws IOException {
        ChatRoom deletedRoom = Server.getRoom(roomToDelete);

        if (deletedRoom != null) {
            if (deletedRoom.getOwner().equals(identity)) {
                // only the owner could delete the room

                moveAllToMainHall(roomToDelete);
                // After deleting room, the users in original room should be moved to mainHall
                deleteRoom(roomToDelete);
                // Reply only to the user that deleted the room with a roomlist

                ArrayList<JSONObject> serverRooms = getRoomListWithCount();
                String deleteResponse = new ServerMessage().roomListMsg(serverRooms);

                getOutput().writeUTF(deleteResponse);
                getOutput().flush();
            } else {
                System.out.println("Client tried to delete an invalid room");
            }
        }
    }



    private void whoRequest(String roomId) throws IOException {
        if (roomId.equals("MainHall")) {

            String[] users = Server.rooms.get(0).getUsers().toArray(new String[Server.rooms.get(0).getUsers().size()]);

            String whoMainHallResponse = new ServerMessage().roomContentsMsg("MainHall", "", users);
            getOutput().writeUTF(whoMainHallResponse);
            getOutput().flush();

        } else {
            ChatRoom roomWho = Server.getRoom(roomId);
            if (roomWho != null) {
                String roomOwner = roomWho.getOwner();
                String[] usersInside = roomWho.getUsers().toArray(new String[Server.getRoom(roomId).getUsers().size()]);

                String whoResponse = new ServerMessage().roomContentsMsg(roomId, roomOwner, usersInside);

                getOutput().writeUTF(whoResponse);
                getOutput().flush();
            }
        }
    }



    private void storeIdentity(String identity, String listenIdentity){
        Server.userIdentities.add(identity+"--listenId:"+listenIdentity);


        //Server.identity = identity;
        //Server.listenIdentity = listenIdentity;

        //建立连接以后直接把此申请连接的peer的监听端口储存在networkList中
        networkList.add(listenIdentity);

        Server.shoutList.add(listenIdentity);

        neighborList.add(listenIdentity);

        //System.out.println("调用storeIdentity方法获得的id是："+identity);

        this.identity = identity;
        //System.out.println("当前连接线程的id是："+this.identity);
        //System.out.println("当前连接线程的listenIdentity是："+listenIdentity);
        this.listenIdentity = listenIdentity;

    }

    private void identityChangeRequest(String newId, String oldId) throws IOException {

        // first time set up identity

        if (newId.equals("")) {
            Server.userIdentities.add(identity);
            String firstIdResponse = new ServerMessage().newIdentityMsg("", identity);

            getOutput().writeUTF(firstIdResponse);
            getOutput().flush();
        } else {

                if (isUsernameInUse(newId)) {
                    String noChangeResponse = new ServerMessage().newIdentityMsg(oldId, oldId);

                    getOutput().writeUTF(noChangeResponse);
                    getOutput().flush();
                    //no update
                }

                else {
                    // First Make integer available for other new guests
                    if (oldId.matches("guest\\d{1,3}")) {
                        String getNumber = oldId.replaceAll("[^0-9]", "");
                        Integer nowAvailableID = Integer.parseInt(getNumber);
                        Server.makeIdAvailable(nowAvailableID);
                    }

                    // update user list
                    for (int i = 0; i < Server.userIdentities.size(); i++) {
                        if (Server.userIdentities.get(i).equals(oldId)) {
                            Server.userIdentities.set(i, newId);
                        }
                    }
                    // update room list
                    for (int i = 0; i < currentRoom.getUsers().size(); i++) {
                        if (currentRoom.getUsers().get(i).equals(oldId)) {
                            currentRoom.getUsers().set(i, newId);
                        }
                    }
                    // check if original user is an owner of current room
                    if (currentRoom.getOwner() != null) {
                        if (currentRoom.getOwner().equals(identity)) {
                            currentRoom.setOwner(newId);
                        }
                    }
                    // Check if original user is owner of any other rooms
                    for (int i = 1; i < Server.rooms.size(); i++) {
                        if (Server.rooms.get(i).getOwner().equals(oldId)) {
                            Server.rooms.get(i).setOwner(newId);
                        }
                    }

                    // broadcast the modification of username
                    identity = newId;
                    String updatedId = new ServerMessage().newIdentityMsg(oldId, identity);
                    Server.announce(updatedId);
                }
        }
    }

    private void checkKicked(String userId){

        try {
            for (int i = 0; i < Server.userIdentities.size(); i++) {
                if (Server.userKicked.get(i).contains(userId)) {
                    //in use
                    try {
                        quitRequest();
                    } catch (IOException e) {
                        //System.out.println("被kicked的用户重新连接server，server试图踢出时发生exception");
                        System.out.println("");
                    }

                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
            //System.out.println("现在黑名单为空");
            System.out.println("");
        }

    }

    public void kickNotice() throws IOException{

        String kickInfo = new ServerMessage().kickMsg();
        getOutput().writeUTF(kickInfo);
        getOutput().flush();
        //quitRequest();
    }

    public void quitRequest() throws IOException {


        for(ClientConnection connection : userThreads){
            if(connection.getIdentity().equals(identity))
            {
                //System.out.println("找到了要清除的thread");
                userThreads.remove(userThreads.indexOf(connection));
            }
        }

        //userThreads.remove(userThreads.get(0));
        //System.out.println("要断连的用户的id是："+identity);

        if(currentRoom!=null)
        {
            currentRoom.removeUser(identity);
        }


        //ConcurrentLinkedQueue<String> newList = new ConcurrentLinkedQueue<String>();
        //newList.addAll(neighborList);
        ArrayList<String> updateUser = new ArrayList<String>();
        ArrayList<String> updateNetwork = new ArrayList<String>();


        while(!neighborList.isEmpty()){
            String element = neighborList.poll();

            if(!element.equals(listenIdentity)) {
                updateUser.add(element);
            }
        }
        for(int i = 0; i < updateUser.size(); i++)
        {
            neighborList.add(updateUser.get(i));

        }

        while(!networkList.isEmpty()){
            String element = networkList.poll();

            if(!element.equals(listenIdentity)) {
                updateNetwork.add(element);
            }
        }
        for(int i = 0; i < updateNetwork.size(); i++)
        {
            networkList.add(updateNetwork.get(i));

        }


        CopyOnWriteArrayList<String> update = new CopyOnWriteArrayList();
        for(int i = 0; i < userIdentities.size(); i++)
        {

            if(!userIdentities.get(i).equals(identity+"--listenId:"+listenIdentity))
            {
                update.add(userIdentities.get(i));
            }
            System.out.println(userIdentities.get(i)+"; ");
        }
        userIdentities = update;


        Server.decreaseGuestCount();
        // Send room change message back to client so it can quit
        String singleRoomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", "quit");
        getOutput().writeUTF(singleRoomChangeMessage);
        getOutput().flush();


    }

    public void migrateRequest(String migrateIp, String migratePort, String roomName) throws IOException {
        //System.out.println("要断连的用户的id是："+identity);

        String migrateMsg = new ServerMessage().migrateMsg(migrateIp, migratePort, roomName);
        getOutput().writeUTF(migrateMsg);
        getOutput().flush();

        //quitRequest();
        //需要先flush一个migrate MSG给client，msg里要包含migrateIp，migratePort和要migrate的roomName


        Server.decreaseGuestCount();

    }



}

