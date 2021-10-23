package Server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {

    private String roomId;
    private String owner;
    private CopyOnWriteArrayList identities = new CopyOnWriteArrayList();
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
        identities.remove(identities.indexOf(userId));
        for (int i = 0; i < clientThreads.size(); i++) {
            if (clientThreads.get(i).getIdentity().equals(userId)) {
                clientThreads.remove(i);
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

    public String[] getUsersArray() {

        return ( String[]) identities.toArray(new String[identities.size()] );

    }

}
