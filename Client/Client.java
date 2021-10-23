package Client;



import Server.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Client{

    @Option(required = true, name = "-h", aliases = {"--host"}, usage = "Host Address")
    private static String host;

    @Option(required = true, name = "-p", aliases = {"--port"}, usage = "Port Address")
    private static int port = 4444;

    private static String identity;

    public static void main(String[] args) throws IOException {

        Socket clientSocket = null;
        try {
            //new Client().init();
            //initiate the client UI
            new Client().doMain(args);
            clientSocket = new Socket(host, port);

            DataInputStream in = new DataInputStream(clientSocket.getInputStream());

            Thread sendingThread = new Thread(new SendThread(clientSocket));
            sendingThread.start();

            // read messages sent from server
            while (true) {

                String response = in.readUTF();

                //utilize json object
                Object obj = JSONValue.parse(response);
                JSONObject jsonMsg = (JSONObject) obj;
                String type = (String) jsonMsg.get("type");

                switch (type) {

                    case "message":
                        messageReply(jsonMsg);
                        break;

                    case "newidentity":
                        newIdentityReply(jsonMsg);
                        break;

                    case "roomchange":
                        //disconnect from chat room
                        if (jsonMsg.get("roomid").toString().equals("")
                                && jsonMsg.get("identity").toString().equals(identity)) {
                            System.out.println("Disconnected from " + clientSocket.getInetAddress());
                            in.close();
                            clientSocket.close();
                            sendingThread.join();

                            System.exit(1);
                        }
                        // normal room change
                        else {
                            String ident = jsonMsg.get("identity").toString();
                            System.out.println(ident + " moves to " + jsonMsg.get("roomid").toString());
                        }
                        break;

                        




                    case "roomcontents":
                        roomContentsReply(jsonMsg);
                        break;

                    case "roomlist":
                        roomListReply(jsonMsg);
                        break;

                    default:
                        System.out.println("Please use a valid message");
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to connect to server");
            if (clientSocket != null) {
                clientSocket.close();
                e.printStackTrace();
            }
        }
    }



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

}
