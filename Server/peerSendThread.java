package Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

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

        this.socket = socket;
        this.ipAddress = ipAddress;
        this.connectPort = connectPort;
        this.listenPort = listenPort;
        this.id = ipAddress + ":" +connectPort;
        this.listenId = ipAddress + ":"+ listenPort;

    }

    @Override
    public void run() {

        try {
            //get outputStream
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            // read users' input
            //读取用户的输入（命令行的输入）
            //System.out.print("[]"+ipAddress+": "+connectPort+">");
            Scanner userInput = new Scanner(System.in);

            String message;

//            String ipAddress = InetAddress.getLocalHost().toString();
//            System.out.println("address1是："+ipAddress);

            //发送当前peer的连接id（作为聊天室的发言id）以及监听地址和端口（供别的peer查看并连接）
            String newHostChangeReq = new peerMessage().newHostChangeRequest(id, listenPort);
            outputStream.writeUTF(newHostChangeReq);
            outputStream.flush();


            // Send new identity message for the first time
            String newIdReq = new peerMessage().newIdentityRequest(id);
            outputStream.writeUTF(newIdReq);
            outputStream.flush();

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






                        //不再需要这个case
//                        case "#identitychange":
//                            try {
//                                String newIdentity = messageTokens[1];
//                                if (newIdentity != null) {
//                                    if (isValidName(newIdentity)) {
//                                        String newIdentityMsg = new peerMessage().newIdentityRequest(newIdentity);
//                                        outputStream.writeUTF(newIdentityMsg);
//                                        outputStream.flush();
//                                    } else {
//                                        System.out.println("Names must be alphanumeric and must not start with a number.");
//                                    }
//                                }
//                            } catch (Exception e) {
//                                System.out.println("No name provided. Please try again.");
//                            }
//                            break;

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

}
