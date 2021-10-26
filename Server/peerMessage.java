package Server;

import org.json.simple.JSONObject;

public class peerMessage {
    //specification of JSON object

    JSONObject jsonMessage;

    public String newHostChangeRequest(String identity, String listenId) {
        //newId message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "hostchange");
        //peer连接另外peer生成的id（连接socket的ip+port）
        jsonMessage.put("identity", identity);

        //peer所监听的地址包括端口号
        //别的peer可以通过此信息连接到该peer
        jsonMessage.put("listenIdentity", listenId);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;

    }


    public String newIdentityRequest(String newIdentity) {
        //newId message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "identitychange");
        jsonMessage.put("identity", newIdentity);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;

    }

    public String joinRoomRequest(String roomId) {
        //join specific room message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "join");
        jsonMessage.put("roomid", roomId);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;

    }

    public String whoRequest(String roomId) {
        //request owner message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "who");
        jsonMessage.put("roomid", roomId);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String listRequest() {
        //获得当前room和其中的人数
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "list");

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String listNeighborsRequest() {
        //获得当前所有连接remote peer的peer
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "listneighbors");

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String createRomRequest(String newRoomId) {
        //create room message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "createroom");
        jsonMessage.put("roomid", newRoomId);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }


    public String deleteRequest(String roomId) {
        //delete request message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "delete");
        jsonMessage.put("roomid", roomId);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String quitRequest() {
        //quit request message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "quit");

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String chatMessage(String msg) {
        //chat message

        jsonMessage = new JSONObject();
        jsonMessage.put("type", "message");
        jsonMessage.put("content", msg);

        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

}
