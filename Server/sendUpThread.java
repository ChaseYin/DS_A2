package Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class sendUpThread implements Runnable {

    Socket socket;
    String ipAddress;
    int connectPort;
    int listenPort;
    String id;
    String currentRoomId;

    //该peer所监听的端口
    String listenId;

    String shouter;
    String msg;

    sendUpThread(Socket socket, String shouterId, String shoutMsg) {
        this.socket = socket;
        this.shouter = shouterId;
        this.msg = shoutMsg;
    }


    @Override
    public void run() {

        try {


            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String message;

            String shoutMsg = new peerMessage().shoutMessage(shouter, msg);
            outputStream.writeUTF(shoutMsg);
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
