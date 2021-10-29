package Server;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static Server.Server.*;
import static java.lang.Integer.parseInt;

public class peerSendThread implements Runnable {

    Socket socket;
    String ipAddress;
    int connectPort;
    int listenPort;

    String id;
    String currentRoomId;

    //该peer所监听的端口
    String listenId;


    peerSendThread(Socket socket, String ipAddress, int connectPort, int listenPort) {

        //this.id = ipAddress+Server.peerConId;


        this.socket = socket;
        this.ipAddress = ipAddress;
        this.connectPort = connectPort;
        this.listenPort = listenPort;
        this.id = ipAddress + ":" +connectPort;
        this.listenId = ipAddress + ":"+ listenPort;

    }



    public void test(){

        System.out.println("");

    }
    @Override
    public void run() {

        try {
            Server.peerConId = connectPort+"";
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            //get outputStream
//            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            // read users' input
            //读取用户的输入（命令行的输入）
            //System.out.print("[]"+ipAddress+": "+connectPort+">");
            Scanner userInput = new Scanner(System.in);

            String message;

//            String ipAddress = InetAddress.getLocalHost().toString();
//            System.out.println("address1是："+ipAddress);

            //发送当前peer的连接id（作为聊天室的发言id）以及监听地址和端口（供别的peer查看并连接）

            //把原来的listenPort改成了listenId
//            String newHostChangeReq = new peerMessage().newHostChangeRequest(id, listenPort);
            String newHostChangeReq = new peerMessage().newHostChangeRequest(id, listenId);
            outputStream.writeUTF(newHostChangeReq);
            outputStream.flush();

//            String searchNeighborsRequestMsg = new peerMessage().listNeighborsRequest();
//            outputStream.writeUTF(searchNeighborsRequestMsg);
//            outputStream.flush();


            // Send new identity message for the first time
//            String newIdReq = new peerMessage().newIdentityRequest(id);
//            outputStream.writeUTF(newIdReq);
//            outputStream.flush();

            // Send roomchange request for the first time, to join MainHall
            //先不发送这个请求
//            String roomChange = new peerMessage().joinRoomRequest("MainHall");
//            outputStream.writeUTF(roomChange);
//            outputStream.flush();


            //后面可能要用到这个命令
            // Display the contents of the entire chat
//            String listMsg = new peerMessage().listRequest();
//            outputStream.writeUTF(listMsg);
//            outputStream.flush();

            // Display contents for MainHall for first-time joiners
//            String whoMsg = new peerMessage().whoRequest("MainHall");
//            outputStream.writeUTF(whoMsg);
//            outputStream.flush();

            // After setting up identity and room, monitor new messages from clients keyboard.
            while (true) {

                message = userInput.nextLine();

                // user inputs commands
                if (message.charAt(0) == '#') {
                    String[] messageTokens = message.split(" ");
                    //get the command
                    String command = messageTokens[0];

                    switch (command) {

                        case "#help":
                           helpMsg();
                            break;

                        case "#join":
                            try {
                                String roomId = messageTokens[1];
                                if (roomId != null && isValidRoomName(roomId)) {
                                    String joinRoomMsg = new peerMessage().joinRoomRequest(roomId);
                                    outputStream.writeUTF(joinRoomMsg);
                                    outputStream.flush();
                                } else {
                                    System.out.println("Invalid room name.");
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you would like to join.");
                            }
                            break;
                        case "#searchnetwork":
                            //users里存连接当前peer的peer
                            try {
                                //应该在最后加一个addNeighbor重新给networkList中加入已连接当前peer下的用户
                                String[] users = networkList.toArray(new String[networkList.size()]);
//                                System.out.println("当前连接本peer的用户有：");
//                                for (int i = 0; i < users.length; i++) {
//                                    System.out.println(" " + users[i] + " ; ");
//                                }

                                while(!networkList.isEmpty()){
                                    try {
                                        int size = networkList.size();

                                        String element = networkList.poll();
                                        String[] portArr = element.split(":");
                                        //System.out.println("需要连接的ip是"+portArr[0]);
                                        //System.out.println("端口号是"+portArr[1]);

                                        //System.out.println("现在有"+size+"个peer需要去search");
                                        //System.out.println("执行第"+num+"次search");

                                        Socket connSocket = null;
                                        connSocket = new Socket(portArr[0], Integer.parseInt(portArr[1]));

                                        DataInputStream ins = new DataInputStream(connSocket.getInputStream());

                                        Thread searchThread = new Thread(new searchNetworkThread(connSocket));
                                        searchThread.start();
                                        System.out.println("Peer ID："+element);

                                        //这里从3改成了2，因为好像并没有case："quit"的时候
                                        for(int n = 0; n < 2;n++){
                                            String response = ins.readUTF();

                                            //utilize json object
                                            Object obj = JSONValue.parse(response);
                                            JSONObject jsonMsg = (JSONObject) obj;
                                            String type = (String) jsonMsg.get("type");

                                            switch (type) {
                                                case "neighborlist":
                                                    //neighborContentsReply(jsonMsg);
                                                    storeNeighbor(jsonMsg);
                                                    break;

                                                //说明当前连接的peer都有哪些room
                                                case "roomlist":
                                                    roomListReply(jsonMsg);
                                                    break;

//                                                case "quit":
//                                                    roomListReply(jsonMsg);
//                                                    break;

                                                default:
                                                    //System.out.println("default情况触发");
                                                    System.out.println("");
                                                    break;
                                            }
                                        }
                                    }catch(EOFException e){
                                        //System.out.println("当前线程quit完毕");
                                        System.out.println("");
                                    }
                                }
                                networkList.addAll(neighborList);


                            }catch(EOFException e){
                                //System.out.println("DataInputStream关闭");
                                System.out.println("");
                            }

                            break;

                        case "#shout":
                            try {
                                String msg = "";
                                for (int i = 1; i < messageTokens.length; i++) {
                                    msg = msg + " " + messageTokens[i];
                                }
                                //String msg = messageTokens[1];
                                //System.out.println("需要shout的信息是："+msg);
                                //System.out.println("需要shout的内容是："+msg);
                                String shoutMsg = new peerMessage().shoutMessage(id,msg);

                                //把这个注释试图解决输出重复的问题，但没有用
                                //当前peer下的shout
                                //Server.shout(shoutMsg);


                                //注释这个试图解决输出重复的问题
                                //先给parent发送shout请求信息，这样子当前peer的neighbor就会收到shout信息
                                outputStream.writeUTF(shoutMsg);
                                outputStream.flush();


                                //System.out.println("shoutList的size："+shoutList.size());

                                //遍历子节点
                                while(!shoutList.isEmpty()){
                                    try {
                                        int size = shoutList.size();

                                        String element = shoutList.poll();
                                        String[] portArr = element.split(":");
                                        //System.out.println("需要连接的ip是"+portArr[0]);
                                        //System.out.println("端口号是"+portArr[1]);

                                        //System.out.println("现在有"+size+"个peer需要去shout");
                                        //System.out.println("执行第"+num+"次search");

                                        Socket shoutSocket = null;
                                        shoutSocket = new Socket(portArr[0], parseInt(portArr[1]));

                                        DataInputStream ins = new DataInputStream(shoutSocket.getInputStream());

                                        Thread shout = new Thread(new shoutThread(shoutSocket,id,msg));
                                        shout.start();
                                        //System.out.println("当前peer为："+element);

                                        //这里从3改成了2，因为好像并没有case："quit"的时候
                                        for(int n = 0; n < 2;n++){
                                            String response = ins.readUTF();

                                            //utilize json object
                                            Object obj = JSONValue.parse(response);
                                            JSONObject jsonMsg = (JSONObject) obj;
                                            String type = (String) jsonMsg.get("type");

                                            switch (type) {
                                                case "neighborlist":
                                                    //neighborContentsReply(jsonMsg);
                                                    storeShoutPeer(jsonMsg);
                                                    break;

                                                //说明当前连接的peer都有哪些room
                                                case "shout":
                                                   System.out.println("");
                                                    break;


                                                default:
                                                    //System.out.println("default情况触发");
                                                    System.out.println("");
                                                    break;
                                            }
                                        }
                                    }catch(EOFException e){
                                        //System.out.println("当前线程quit完毕");
                                        System.out.println("");
                                    }
                                }

                                shoutList.addAll(neighborList);





                            } catch (Exception e) {
                                System.out.println("Please provide the message would like to shout.");
                            }
                            break;

                        case "#who":
                            try {
                                String room = messageTokens[1];
                                if (room != null) {
                                    String whoRequestMsg = new peerMessage().whoRequest(room);
                                    outputStream.writeUTF(whoRequestMsg);
                                    outputStream.flush();
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you wish to inspect");
                            }
                            break;

                        case "#list":

                            String listRequestMsg = new peerMessage().listRequest();
                            outputStream.writeUTF(listRequestMsg);
                            outputStream.flush();
                            //System.out.println("执行peerSendThread中的远程#list命令");
                            break;

                        case "#listneighbors":

                            String listNeighborsRequestMsg = new peerMessage().listNeighborsRequest();
                            outputStream.writeUTF(listNeighborsRequestMsg);
                            outputStream.flush();
                            break;

                            //目前主要修改这个命令

                        case "#createroom":
                            System.out.println("When you connected to other peer, you cannot process createroom order locally and remotely");
//                            try {
//                                String newRoom = messageTokens[1];
//                                if ((newRoom != null)
//                                        && isValidRoomName(newRoom)) {
//                                    String createRequestMsg = new peerMessage().createRomRequest(newRoom);
//                                    outputStream.writeUTF(createRequestMsg);
//                                } else {
//                                    System.out.println("Rooms must contain number and letter with at least 3 " + "characters and no more than 16 characters");
//                                }
//                            } catch (Exception e) {
//                                System.out.println("Please input a room name");
//                            }
                            break;

                        case "#delete":
                            System.out.println("When you connected to other peer, you cannot process deleteroom order locally and remotely");
//                            try {
//
//                                String deleteRequestMsg = new peerMessage().deleteRequest(messageTokens[1]);
//                                outputStream.writeUTF(deleteRequestMsg);
//                                outputStream.flush();
//
//                            } catch (Exception e) {
//                                System.out.println("Please input the name of the room you wish to delete");
//                            }
                            break;

                        case "#quit":

                            String quitRequest = new peerMessage().quitRequest();
                            outputStream.writeUTF(quitRequest);
                            outputStream.flush();

                            //userInput.close();
                            throw new InterruptedException();

                        default:
                            System.out.println("You may have typed a command incorrectly");
                            break;
                    }
                }
                //  send normal message
                //发送普通的聊天信息
                else {

                    String jsonChatMessage = new peerMessage().chatMessage(message);
//                    System.out.println("Currently you do not join any chatroom of the host peer, so your word will not" +
//                            "be seen by others");

                    outputStream.writeUTF(jsonChatMessage);
                    outputStream.flush();

                }
            }
        } catch (IOException | InterruptedException | NoSuchElementException e) {
            //System.out.println("IO error");
            //e.printStackTrace();
            //System.out.println("结束该线程");

            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidName(String input) {
        //check the name if meet the requirements
        if ((input.length() >= 3 && input.length() <= 16) && (input.matches("[A-Za-z0-9]+")) &&
                (!Character.isDigit(input.charAt(0))))
        {
            return true;

        } else {

            return false;

        }
    }

    private boolean isValidRoomName(String input) {
        //check the name if meet the requirements
        if ((input.length() >= 3 && input.length() <= 32) && (input.matches("[A-Za-z0-9]+"))
                && (!Character.isDigit(input.charAt(0)))
        ) {
            return true;
        } else {
            return false;
        }
    }

    public DataOutputStream getOutput() throws IOException {

        return new DataOutputStream(socket.getOutputStream());

    }

    private static void storeShoutPeer(JSONObject jsonMsg) {

//        String currentRoom = jsonMsg.get("roomid").toString();
        //get all identities of the room
        JSONArray jsonRoomMembers = (JSONArray) jsonMsg.get("neighbors");

        ArrayList<String> roomMembers = new ArrayList<>();

        for (int i = 0; i < jsonRoomMembers.size(); i++) {
            //use msg from server to add user into rooms
            roomMembers.add(jsonRoomMembers.get(i).toString());

            shoutList.add(jsonRoomMembers.get(i).toString());
        }

    }

    private static void helpMsg() {

        System.out.println("Following is the examples of remote command :");
        System.out.println("#join roomName");
        System.out.println("#who roomName");
        System.out.println("#shout shoutMsg");
        System.out.println("#listneighbors");
        System.out.println("#list");
        System.out.println("#quit");

        System.out.println();


    }

    private static void storeNeighbor(JSONObject jsonMsg) {

//        String currentRoom = jsonMsg.get("roomid").toString();
        //get all identities of the room
        JSONArray jsonRoomMembers = (JSONArray) jsonMsg.get("neighbors");

        ArrayList<String> roomMembers = new ArrayList<>();

        for (int i = 0; i < jsonRoomMembers.size(); i++) {
            //use msg from server to add user into rooms
            roomMembers.add(jsonRoomMembers.get(i).toString());

            networkList.add(jsonRoomMembers.get(i).toString());
        }

    }

    private static void roomListReply(JSONObject jsonMsg) {
        JSONArray roomList = (JSONArray) jsonMsg.get("rooms");
        if(roomList.size()==0)
        {
            System.out.println("This peer does not create any rooms yet");
        }else{
            for (int i = 0; i < roomList.size(); i++) {
                //print the room id and count of each room
                JSONObject singleRoom = (JSONObject) roomList.get(i);
                System.out.print(singleRoom.get("roomid").toString() + ": ");
                System.out.print(singleRoom.get("count").toString() + " guest/s");
                System.out.println();
                System.out.println();

            }
        }

    }


}
