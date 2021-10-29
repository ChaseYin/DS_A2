package Server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {

    private String roomId;
    private String owner;
    public CopyOnWriteArrayList identities = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<ClientConnection> clientThreads = new CopyOnWriteArrayList<>();

    //chatroom constructor
    ChatRoom(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {

        return owner;
    }



    public CopyOnWriteArrayList<String> getUsers() {

        return identities;

    }

    public void setOwner(String owner) {

        this.owner = owner;
    }

    public void addClient(ClientConnection clientConnect) {

        clientThreads.add(clientConnect);
    }

    public CopyOnWriteArrayList<ClientConnection> getClientThreads() {
        return clientThreads;
    }

    public void removeUser(String userId) {
        //System.out.println("要移除的用户是："+userId);

        if(identities.size()==0)
        {
            //System.out.println("当前room还没有人");
            System.out.println("Current room contains nobody");
        }
        else{


            identities.remove(identities.indexOf(userId));
            //System.out.println("当前room中包含的用户有"+identities.size()+"个");


            for (int i = 0; i < clientThreads.size(); i++) {
                if (clientThreads.get(i).getIdentity().equals(userId)) {
                    clientThreads.remove(i);
                }
            }
        }

    }

    public void broadcastToRoom(String message) throws IOException {
        //遍历每一个room
        for (ClientConnection clientConnect : clientThreads) {
            //broadcast message to room
            Thread send = new Thread(new ServerSendThread(clientConnect.getOutput(), message));

            send.start();
        }
    }

    public void shoutToRoom(String message) throws IOException {
        //System.out.println("进入shoutToRoom方法");
        //遍历每一个room
//        for (ClientConnection clientConnect : clientThreads) {
//            System.out.println("当前room的线程包含："+clientConnect.getIdentity());
//
//        }

        for (ClientConnection clientConnect : clientThreads) {
           // System.out.println("正在shout的线程是："+clientConnect.getIdentity());

            //broadcast message to room
            Thread send = new Thread(new ServerSendThread(clientConnect.getOutput(), message));

            send.start();
        }
    }


    public String[] getUsersArray() {

        return ( String[]) identities.toArray(new String[identities.size()] );

    }

}
