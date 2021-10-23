package Server;

import java.io.DataOutputStream;
import java.io.IOException;

public class ServerSendThread implements Runnable {

    DataOutputStream out;
    String message;

    ServerSendThread(DataOutputStream out, String message) {
        //constructor
        this.out = out;
        this.message = message;
    }

    @Override
    public void run() {

        try {
            //这里的message不能改变
            System.out.print("这里是serverSendThread");
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
