package Server;

import Client.ClientMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Integer.parseInt;
//import static jdk.internal.logger.DefaultLoggerFinder.SharedLoggers.system;

public class peerListenThread implements Runnable {

    Socket socket;
    String peerId;//address+监听的端口
    int listenPort;//peer监听的端口
    String ip;
    String roomId="";

    private static String identity;
    public static  Boolean connected = false;




    peerListenThread(String ipAddress, int listenPort) {

        this.ip = ipAddress;
        this.listenPort = listenPort;
        this.peerId = ipAddress + listenPort;


    }

    //这个thread要实现的功能是监听peer的local command（只处理create room， delete room）
    @Override
    public void run() {

        try {
            //get outputStream
           // DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            // read users' input
            //读取用户的输入（命令行的输入）

            Scanner userInput = new Scanner(System.in);

            String message;
            System.out.println("成功启动本地监听线程");

            String ipAddress = InetAddress.getLocalHost().toString();
            System.out.println("address1是："+ipAddress);
            // Send new identity message for the first time
//            String newIdReq = new peerMessage().newIdentityRequest("");
//            outputStream.writeUTF(newIdReq);
//            outputStream.flush();

            // Send roomchange request for the first time, to join MainHall
//            String roomChange = new peerMessage().joinRoomRequest("MainHall");
//            outputStream.writeUTF(roomChange);
//            outputStream.flush();

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
                System.out.println("Please note that if you want to assign specific port of connection socket to the other" +
                        "peer, try #connect hostAddress hostPort -connectionPort connectionPort to assign your own connectionPort\n");
                System.out.println("Otherwise your connection port(also refer to your id) will be automatically generated by system");


                System.out.print(">");message = userInput.nextLine();

                // user inputs commands
                if (message.charAt(0) == '#') {
                    String[] messageTokens = message.split(" ");
                    //get the command
                    String command = messageTokens[0];


                    switch (command) {

                        //后续可能需要这个命令列出当前peer已经create的room列表
                        case "#list":

                            String listRequestMsg = new peerMessage().listRequest();
                            ArrayList<JSONObject> roomsCount = Server.getRoomListWithCount();
                            System.out.println("当前peer下的所有room为："+roomsCount);
//                            outputStream.writeUTF(listRequestMsg);
//                            outputStream.flush();
                            break;

                        case "#kick":
                            try {
                                String userToKick = messageTokens[1];
                                Server.addKickUsers(userToKick);
                            } catch (Exception e) {
                                System.out.println("Please provide the the user you wish to kick followed by #kick");
                            }
                            break;



                        case "#connect":
                            try {
                                if(messageTokens.length==5)
                                {
                                    String connectAddress = messageTokens[1];
                                    //要连接的server的监听端口
                                    String connectPort = messageTokens[2];

                                    //-connectionPort后的数字指定peer的connect端口
                                    //这里需要加一个try-catch结构去触发如果用户输错命令（少输入元素），则提醒用户try again
                                    String conPort = messageTokens[4];


                                    System.out.println("the id of this peer is :"+ipAddress+":"+conPort);

                                    //与远端remote peer建立连接，并且监听response
                                    Socket connectSocket = null;
                                    connectSocket = new Socket(connectAddress, parseInt(connectPort));
                                    DataInputStream in = new DataInputStream(connectSocket.getInputStream());

                                    Thread sendingThread = new Thread(new peerSendThread(connectSocket,ipAddress,parseInt(conPort),listenPort));
                                    sendingThread.start();
                                    connected = true;



                                    try {
                                        //在这里读连接到的peer（server）返回的response
                                        while (true) {
                                            System.out.print("[" + roomId + "]" + ipAddress + ": " + conPort + ">");
                                            //System.out.print(ipAddress+": "+conPort+">");
                                            String response = in.readUTF();

                                            //utilize json object
                                            Object obj = JSONValue.parse(response);
                                            JSONObject jsonMsg = (JSONObject) obj;
                                            String type = (String) jsonMsg.get("type");

                                            switch (type) {

                                                case "message":
                                                    messageReply(jsonMsg);
                                                    break;

                                                case "neighborlist":
                                                    neighborContentsReply(jsonMsg);
                                                    break;

                                                //peer不可以修改自己的id
//                                        case "newidentity":
//                                            newIdentityReply(jsonMsg);
//                                            break;

                                                case "roomchange":
                                                    //disconnect from chat room

                                                    System.out.println("formerRoom是："+jsonMsg.get("former").toString());
                                                    System.out.println("newRoom是："+jsonMsg.get("roomid").toString());
                                                    System.out.println("id是："+jsonMsg.get("identity").toString());
                                                    //初次连接
//                                                    if (jsonMsg.get("roomid").toString().equals("")
//                                                            && jsonMsg.get("former").toString().equals("")) {
//
//                                                        System.out.println("The connection has been successfully established! ");
//
//                                                    }

                                                    //需要移动到的room的id为空，代表断连或者room被删除
//                                                    if (jsonMsg.get("roomid").toString().equals("quit")
//                                                            && jsonMsg.get("identity").toString().equals(identity)) {
                                                    if (jsonMsg.get("roomid").toString().equals("quit")
                                                            ) {

                                                            System.out.println("Disconnected from " + connectSocket.getInetAddress());
                                                            in.close();
                                                            connectSocket.close();
                                                            sendingThread.join();
                                                            connected = false;

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
                                                    System.out.println("The peer you connected cannot process your command");
                                                    break;
                                            }
                                        }
                                        //System.out.println("跳出循环！！！！");

                                    }catch (EOFException | InterruptedException e){
                                        System.out.println("捕获EOF");

                                        Thread listeningThread = new Thread(new peerListenThread(ipAddress,listenPort));
                                        listeningThread.start();
                                        Thread.currentThread().interrupt();
                                    }


                                    //sendingThread.join();
                                }
                                else{
                                    System.out.println("the connect order is not right,please use #connect remotepeer portnumber -cp yourIdPort to connect");
                                }




                            } catch (EOFException e) {
                                //当输入#quit的时候会触发一个EOFException，因为in被关闭了但是马上又被while（true）拿去监听
                                //不过正常功能好像并不影响
                                System.out.println("Errors happened when try to connect the remote peer");
                                e.printStackTrace();
                            }


                            break;


                        //目前主要修改这个命令

                        case "#createroom":
                            try {
                                String newRoom = messageTokens[1];

                                if(connected)
                                {
                                    System.out.println("When you connected to other peer, local command cannot be processed");
                                }
                                else{
                                    //System.out.println("当前还没有连接到别的peer");
                                    if ((newRoom != null)
                                        && isValidRoomName(newRoom)) {
                                    Server.createLocalRoom(newRoom, peerId);
                                    //String createRequestMsg = new peerMessage().createRomRequest(newRoom);
//                                    createRoom(createRequestMsg);
                                } else {
                                    System.out.println("Rooms must contain number and letter with at least 3 " + "characters and no more than 16 characters");
                                }

                                }


                            } catch (Exception e) {
                                System.out.println("Please input a room name");
                            }
                            break;

                        case "#delete":
                            try {

                                //String deleteRequestMsg = new peerMessage().deleteRequest(messageTokens[1]);
                                Server.deleteRoom(messageTokens[1]);
//                                outputStream.writeUTF(deleteRequestMsg);
//                                outputStream.flush();

                            } catch (Exception e) {
                                System.out.println("Please input the name of the room you wish to delete");
                            }
                            break;

                        default:
                            System.out.println("You may have typed a command incorrectly");
                            break;
                    }
                }
                //  send normal message
                //发送普通的聊天信息
                else {
                        System.out.println("Currently you do not connect to any peers yet, plase try '#connect' command ");
//                    String jsonChatMessage = new peerMessage().chatMessage(message);
//                    outputStream.writeUTF(jsonChatMessage);
//                    outputStream.flush();

                }
            }
        } catch (Exception e) {
            System.out.println("Error happens");
            e.printStackTrace();
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



    //原来client类里的方法
    private static void messageReply(JSONObject jsonMsg) {
        //display clients' chat content

        System.out.print("[]"+jsonMsg.get("identity").toString() + " > " + jsonMsg.get("content").toString());
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
