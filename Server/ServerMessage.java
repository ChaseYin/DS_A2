package Server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ServerMessage {

    JSONObject jsonMessage;

    public String newIdentityMsg(String oldId, String newId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "newidentity");
        jsonMessage.put("former", oldId);
        jsonMessage.put("identity", newId);
        return jsonMessage.toString();
    }

    public String neighborMsg(String[] identities) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "neighborlist");
        JSONArray jsonIdentities = new JSONArray();
        for (String identity : identities) {
            jsonIdentities.add(identity);
        }

        jsonMessage.put("neighbors", jsonIdentities);
        return jsonMessage.toString();
    }

    public String roomChangeMsg(String identity, String previousRoom, String newRoom) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "roomchange");
        jsonMessage.put("identity", identity);
        jsonMessage.put("former", previousRoom);
        jsonMessage.put("roomid", newRoom);
        return jsonMessage.toString();
    }

    public String roomChangeCheckMsg(String roomId, String result) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "roomchangecheck");
        jsonMessage.put("roomId", roomId);
        jsonMessage.put("result", result);
        return jsonMessage.toString();
    }

    public String kickMsg() {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "kickInfo");

        return jsonMessage.toString();
    }

    public String migrateMsg(String futureIp, String futurePort, String roomName) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "migrate");
        jsonMessage.put("migrateIp", futureIp);
        jsonMessage.put("migratePort", futurePort);
        jsonMessage.put("migrateRoom", roomName);
        return jsonMessage.toString();
    }

    public String shoutMsg(String msg) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "shout");
        jsonMessage.put("success", msg);

        return jsonMessage.toString();
    }

    public String roomContentsMsg(String roomId, String owner, String[] identities) {
        jsonMessage = new JSONObject();
        JSONArray identyList = new JSONArray();
        jsonMessage.put("type", "roomcontents");
        jsonMessage.put("roomid", roomId);

        JSONArray jsonIdentities = new JSONArray();
        for (String identity : identities) {
            jsonIdentities.add(identity);
        }

        jsonMessage.put("identities", jsonIdentities);
        jsonMessage.put("owner", owner);
        return jsonMessage.toString();
    }

    public String roomListMsg(ArrayList<JSONObject> rooms) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "roomlist");

        JSONArray roomList = new JSONArray();
        for (JSONObject jo : rooms) {
            roomList.add(jo);
        }

        jsonMessage.put("rooms", roomList);

        return jsonMessage.toString();
    }

}
