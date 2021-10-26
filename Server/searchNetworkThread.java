package Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class searchNetworkThread implements Runnable {

    Socket socket;
    String ipAddress;
    int connectPort;
    int listenPort;
    String id;
    String currentRoomId;

    //该peer所监听的端口
    String listenId;

    searchNetworkThread(Socket socket) {

        this.socket = socket;
//        this.ipAddress = ipAddress;
//        //this.connectPort = connectPort;
//        this.listenPort = listenPort;
//        //this.id = ipAddress + ":" +connectPort;
//        this.listenId = ipAddress + ":"+ listenPort;

    }


    @Override
    public void run() {

        try {


            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String message;


            String listNeighborsRequestMsg = new peerMessage().listNeighborsRequest();
            outputStream.writeUTF(listNeighborsRequestMsg);
            outputStream.flush();

            String listMsg = new peerMessage().listRequest();
            outputStream.writeUTF(listMsg);
            outputStream.flush();

            String quitRequest = new peerMessage().quitRequest();
            outputStream.writeUTF(quitRequest);
            outputStream.flush();

//            Thread.currentThread().interrupt();


        } catch (IOException e) {
            //System.out.println("IO error");
            e.printStackTrace();
            //Thread.currentThread().interrupt();
        }
    }



}
