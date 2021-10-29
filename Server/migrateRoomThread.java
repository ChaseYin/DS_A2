package Server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class migrateRoomThread implements Runnable {

    Socket socket;
    String ipAddress;
    int connectPort;
    int listenPort;
    String id;
    String currentRoomId;
    String roomId="";

    //该peer所监听的端口
    String listenId;

    String migrateRoom;

    migrateRoomThread(Socket socket, String ipAddress, int connectPort, int listenPort, String migrateRoom) {

        this.socket = socket;
        this.ipAddress = ipAddress;
        this.connectPort = connectPort;
        this.listenPort = listenPort;
        this.id = ipAddress + ":" +connectPort;
        this.listenId = ipAddress + ":"+ listenPort;

        this.migrateRoom = migrateRoom;

    }



    public void test(){

        System.out.println("");

    }
    @Override
    public void run() {
        Scanner userInput = new Scanner(System.in);

        System.out.println("------------这里是migrateRoomThread的console----------");
        System.out.println("------------migrateRoomThread开始监听----------");

        try {
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String response = inStream.readUTF();



            String ipAddress = InetAddress.getLocalHost().toString();
            String[] arr = ipAddress.split("/");
            System.out.println("第一个参数是："+arr[0]);
            System.out.println("第二个参数是："+arr[1]);

            String ip = arr[1];

            String newHostChangeReq = new peerMessage().newHostChangeRequest(id, listenId);
            outputStream.writeUTF(newHostChangeReq);
            outputStream.flush();

            String newIdReq = new peerMessage().newIdentityRequest(id);
            outputStream.writeUTF(newIdReq);
            outputStream.flush();

            String migrateMsg = new peerMessage().migrateRequestMsg(migrateRoom);
            outputStream.writeUTF(migrateMsg);
            outputStream.flush();


//            String joinRoomMsg = new peerMessage().joinRoomRequest(migrateRoom);
//            outputStream.writeUTF(joinRoomMsg);
//            outputStream.flush();


            //utilize json object
            Object obj = JSONValue.parse(response);
            JSONObject jsonMsg = (JSONObject) obj;
            String type = (String) jsonMsg.get("type");
            // After setting up identity and room, monitor new messages from clients keyboard.
            while (true) {
                String message = userInput.nextLine();
                System.out.print("migrateThread->[" + roomId + "]" + ipAddress + ": " + connectPort + ">");
                //System.out.print(ipAddress+": "+conPort+">");

                // user inputs commands
                if (message.charAt(0) == '#') {
                    String[] messageTokens = message.split(" ");
                    //get the command
                    String command = messageTokens[0];

                    switch (command) {

                        case "#join":
                            try {
                                String roomId = messageTokens[1];
                                if (roomId != null && isValidRoomName(roomId)) {
                                    String joinMsg = new peerMessage().joinRoomRequest(roomId);
                                    outputStream.writeUTF(joinMsg);
                                    outputStream.flush();
                                } else {
                                    System.out.println("Invalid room name.");
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you would like to join.");
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
                            System.out.println("migthread->执行远程#list命令");
                            break;

                        case "#listneighbors":

                            String listNeighborsRequestMsg = new peerMessage().listNeighborsRequest();
                            outputStream.writeUTF(listNeighborsRequestMsg);
                            outputStream.flush();
                            break;

                        //目前主要修改这个命令

                        case "#createroom":
                            System.out.println("When you connected to other peer, you cannot process createroom order locally and remotely");

                            break;

                        case "#delete":
                            System.out.println("When you connected to other peer, you cannot process deleteroom order locally and remotely");

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


                switch (type) {

                    case "message":
                        System.out.println("收到message类型的response");
                        messageReply(jsonMsg);
                        break;

                    case "migrate":
                        String migratePeerIp = jsonMsg.get("migrateIp").toString();
                        String migratePeerPort = jsonMsg.get("migratePort").toString();
                        String migrateRoom = jsonMsg.get("migrateRoom").toString();

                        try{
                            System.out.println("Disconnected from " + socket.getInetAddress());
                            inStream.close();
                            socket.close();

                            //sendingThread.join();

                            //connected = false;

                            //中断与remote peer通信的连接线程
                            throw new InterruptedException();
                        }catch(InterruptedException e){
                            System.out.println("成功中断与previous peer的连接");
                        }

                        Socket migrateSocket = null;
                        migrateSocket = new Socket(migratePeerIp, parseInt(migratePeerPort));


                        Thread migrateThread = new Thread(new migrateRoomThread(migrateSocket,ip,connectPort,listenPort,migrateRoom));
                        migrateThread.start();

                        //messageReply(jsonMsg);
                        break;

                    case "neighborlist":
                        System.out.println("收到neighbor类型的response");
                        neighborContentsReply(jsonMsg);
                        break;


                    case "roomchange":
                        //disconnect from chat room
                        System.out.println("MigrateThread下的roomChange消息");
                        System.out.println("formerRoom是："+jsonMsg.get("former").toString());
                        System.out.println("newRoom是："+jsonMsg.get("roomid").toString());
                        System.out.println("id是："+jsonMsg.get("identity").toString());
                        //初次连接
                        if (jsonMsg.get("roomid").toString().equals("quit")
                        ) {

                            System.out.println("Disconnected from " + socket.getInetAddress());
                            inStream.close();
                            socket.close();
                            //sendingThread.join();
                            //connected = false;

                            //中断与remote peer通信的连接线程
                            throw new InterruptedException();
                            //System.exit(1);

                        }
                        // normal room change
                        else {
                            if(jsonMsg.get("roomid").toString().equals(""))
                            {
                                System.out.println("第一次连接成功建立");
                            }else{
                                roomId = jsonMsg.get("roomid").toString();
//                                                    System.out.print("[]"+ipAddress+": "+conPort+">");
                                String ident = jsonMsg.get("identity").toString();
                                //向当前room公告x移动到了room X
                                System.out.println(ident + " moves to " + jsonMsg.get("roomid").toString());
                            }


                        }
                        break;


                    //说明当前room都有些谁
                    case "roomcontents":
                        roomContentsReply(jsonMsg);
                        break;

                    //说明当前连接的peer都有哪些room
                    case "roomlist":
                        roomListReply(jsonMsg);
                        break;

                    default:
                        System.out.println("Default condition in migrateRoomThread occured");
                        break;
                }





            }




        } catch (IOException | InterruptedException e) {
            //System.out.println("IO error");
            e.printStackTrace();
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

    private static void messageReply(JSONObject jsonMsg) {
        //display clients' chat content

        System.out.print(jsonMsg.get("identity").toString() + " > " + jsonMsg.get("content").toString());
        System.out.println();

    }


    private static void neighborContentsReply(JSONObject jsonMsg) {

//        String currentRoom = jsonMsg.get("roomid").toString();
        //get all identities of the room
        JSONArray jsonRoomMembers = (JSONArray) jsonMsg.get("neighbors");

        ArrayList<String> roomMembers = new ArrayList<>();

        for (int i = 0; i < jsonRoomMembers.size(); i++) {
            //use msg from server to add user into rooms
            roomMembers.add(jsonRoomMembers.get(i).toString());

        }


        System.out.print("neighbor contains ");

        for (String member : roomMembers) {
            System.out.print(member + "; ");
        }
        System.out.println();



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




}
