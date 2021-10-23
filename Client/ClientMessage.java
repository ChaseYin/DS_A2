package Client;

import org.json.simple.JSONObject;

public class ClientMessage {
    //specification of JSON object

    JSONObject jsonMessage;


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
        //list request message
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "list");

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
