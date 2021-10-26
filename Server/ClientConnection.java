package Server;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;



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
            } catch (SocketException e) {
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
                                currentRoom.broadcastToRoom(jsonMsg.toString());
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
            String[] users = Server.networkList.toArray(new String[Server.networkList.size()]);

            if(users.length==0)
            {
                System.out.println("现在此peer还没有neighbor");
            }else{
                //System.out.println("获得的已连接用户是：" + users[0]);
                String whoMainHallResponse = new ServerMessage().neighborMsg(users);
                getOutput().writeUTF(whoMainHallResponse);
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
        if (roomId != null && !roomId.equals("MainHall")) {
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
        if (roomId.equals("MainHall")) {
            if (!isAlreadyInRoom(identity, roomId)) {

                if (currentRoom != null) {
                    removeClientFromPreviousRoom(currentRoom.getRoomId(), identity);

                    // Delete previous room if they are the owner and no one is in it
                    deleteRoomIfOwner(identity, currentRoom);
                }

                // Add them to the room
                ChatRoom mainHall = Server.rooms.get(0);
                currentRoom = mainHall;
                currentRoom.getClientThreads().add(ClientConnection.this);        // Add new thread to chatroom(mainHall)
                currentRoom.getUsers().add(ClientConnection.this.getIdentity()); // Add username to list of mainHall

                // Send room change message to all in the room
                String response = new ServerMessage().roomChangeMsg
                        (mainHall.getUsers().get(mainHall.getUsers().size() - 1), "", "MainHall");
                Server.rooms.get(0).broadcastToRoom(response);

                // List the people in Main Hall to the single user
                String[] mainHallClients = mainHall.getUsersArray();
                String listMessage = new ServerMessage().roomContentsMsg("MainHall", "", mainHallClients);
                getOutput().writeUTF(listMessage);
                getOutput().flush();
            }
        } else {

            ClientConnection thisConnection = ClientConnection.this;

                if (!isAlreadyInRoom(identity, roomId)) {

                    joinRoom(roomId);

                }




            }
    }

    private void joinRoom(String roomId) throws IOException {
        ChatRoom room = Server.getRoom(roomId);

        if (room != null) {

            if(currentRoom == null){
                // Broadcast changes to rooms
                String roomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", roomId);

                //currentRoom.broadcastToRoom(roomChangeMessage);
                room.broadcastToRoom(roomChangeMessage);

                // Remove from current room
                //currentRoom.removeUser(identity);

                // Delete previous room if they are the owner and no one is in it
                //deleteRoomIfOwner(identity, currentRoom);

                // Put user in the new room
                currentRoom = room;

                // Record them as now being in the new room
                currentRoom.getUsers().add(identity);
                currentRoom.getClientThreads().add(ClientConnection.this);
            }else{
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

            // Put user in the new room
            currentRoom = room;

            // Record them as now being in the new room
            currentRoom.getUsers().add(identity);
            currentRoom.getClientThreads().add(ClientConnection.this);

        }
    }

    private void createRoomRequest(String newRoomId) throws IOException {
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
            System.out.println("成功创建room:"+newRoomId);

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
        Server.userIdentities.add(identity+"--listenPort:"+listenIdentity);

        //建立连接以后直接把此申请连接的peer的监听端口储存在networkList中
        Server.networkList.add(listenIdentity);

        System.out.println("调用storeIdentity方法获得的id是："+identity);

        this.identity = identity;
        System.out.println("当前连接线程的id是："+this.identity);
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
                        System.out.println("被kicked的用户重新连接server，server试图踢出时发生exception");
                    }

                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
            System.out.println("现在黑名单为空");
        }




    }
    public void quitRequest() throws IOException {

//        if (identity.matches("guest\\d{1,3}")) {
//            String digit = identity.replaceAll("[A-Za-z]", "");
//            Integer availableId = Integer.parseInt(digit);
//            Server.makeIdAvailable(availableId);
//        }
        // Remove from current room
        System.out.println("要断连的用户的id是："+identity);
        //currentRoom.removeUser(identity);

        // Send room change message, new room is an empty string

        //当前情况下，如果刚连接到remote peer就quit的话会报错，因为currentroom为null，后面可以改一下
        //String roomChangeMsg = new ServerMessage().roomChangeMsg(identity, currentRoom.getRoomId(), "");
        //currentRoom.broadcastToRoom(roomChangeMsg);

        // If they are the owner of any rooms, set the owner to be an empty string

        //删掉所有用户名下的room
        //removeAnyRoomOwnerships(identity);


        Server.decreaseGuestCount();
        // Send room change message back to client so it can quit
        String singleRoomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", "quit");
        getOutput().writeUTF(singleRoomChangeMessage);
        getOutput().flush();
    }


}

