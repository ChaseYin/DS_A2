package Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class SendThread implements Runnable {

    Socket socket;

    SendThread(Socket socket) {

        this.socket = socket;

    }

    @Override
    public void run() {

        try {
            //get outputStream
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            // read users' input
            Scanner userInput = new Scanner(System.in);

            String message;

            // Send new identity message for the first time
            String newIdReq = new ClientMessage().newIdentityRequest("");
            outputStream.writeUTF(newIdReq);
            outputStream.flush();

            // Send roomchange request for the first time, to join MainHall
            String roomChange = new ClientMessage().joinRoomRequest("MainHall");
            outputStream.writeUTF(roomChange);
            outputStream.flush();

            // Display the contents of the entire chat
            String listMsg = new ClientMessage().listRequest();
            outputStream.writeUTF(listMsg);
            outputStream.flush();

            // Display contents for MainHall for first-time joiners
            String whoMsg = new ClientMessage().whoRequest("MainHall");
            outputStream.writeUTF(whoMsg);
            outputStream.flush();

            // After setting up identity and room, monitor new messages from clients keyboard.
            while (true) {

                message = userInput.nextLine();

                // user inputs commands
                if (message.charAt(0) == '#') {


                    String[] messageTokens = message.split(" ");
                    //get the command
                    String command = messageTokens[0];

                    switch (command) {

                        case "#quit":

                            String quitRequest = new ClientMessage().quitRequest();
                            outputStream.writeUTF(quitRequest);
                            outputStream.flush();
                            break;

                        case "#identitychange":
                            try {
                                String newIdentity = messageTokens[1];
                                if (newIdentity != null) {
                                    if (isValidName(newIdentity)) {
                                        String newIdentityMsg = new ClientMessage().newIdentityRequest(newIdentity);
                                        outputStream.writeUTF(newIdentityMsg);
                                        outputStream.flush();
                                    } else {
                                        System.out.println("Names must be alphanumeric and must not start with a number.");
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("No name provided. Please try again.");
                            }
                            break;

                        case "#join":
                            try {
                                String roomId = messageTokens[1];
                                if (roomId != null && isValidRoomName(roomId)) {
                                    String joinRoomMsg = new ClientMessage().joinRoomRequest(roomId);
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
                                    String whoRequestMsg = new ClientMessage().whoRequest(room);
                                    outputStream.writeUTF(whoRequestMsg);
                                    outputStream.flush();
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you wish to inspect");
                            }
                            break;

                        case "#list":

                            String listRequestMsg = new ClientMessage().listRequest();
                            outputStream.writeUTF(listRequestMsg);
                            outputStream.flush();
                            break;

                        case "#createroom":
                            try {
                                String newRoom = messageTokens[1];
                                if ((newRoom != null)
                                        && isValidRoomName(newRoom)) {
                                    String createRequestMsg = new ClientMessage().createRomRequest(newRoom);
                                    outputStream.writeUTF(createRequestMsg);
                                } else {
                                    System.out.println("Rooms must contain number and letter with at least 3 " + "characters and no more than 16 characters");
                                }
                            } catch (Exception e) {
                                System.out.println("Please input a room name");
                            }
                            break;

                        case "#delete":
                            try {

                                String deleteRequestMsg = new ClientMessage().deleteRequest(messageTokens[1]);
                                outputStream.writeUTF(deleteRequestMsg);
                                outputStream.flush();

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
                else {

                    String jsonChatMessage = new ClientMessage().chatMessage(message);
                    outputStream.writeUTF(jsonChatMessage);
                    outputStream.flush();

                }
            }
        } catch (IOException e) {
            System.out.println("IO error");
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

}
