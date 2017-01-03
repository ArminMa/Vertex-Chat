package model;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * Created by sebastian markström on 2017-01-02.
 */
public class AllMessagesPojo {
    private UserInfo sender;
    private UserInfo receiver;

    public AllMessagesPojo() {
    }

    public AllMessagesPojo(UserInfo sender, UserInfo receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public AllMessagesPojo(JsonObject json){
        JsonObject user = json.getJsonObject("sender");
        sender = Json.decodeValue(user.toString(), UserInfo.class);
        user = json.getJsonObject("receiver");
        receiver = Json.decodeValue(user.toString(), UserInfo.class);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("sender", sender.toJson())
                .put("receiver", receiver.toJson());
        return json;
    }

    public UserInfo getSender() {
        return sender;
    }

    public void setSender(UserInfo sender) {
        this.sender = sender;
    }

    public UserInfo getReceiver() {
        return receiver;
    }

    public void setReceiver(UserInfo receiver) {
        this.receiver = receiver;
    }
}
