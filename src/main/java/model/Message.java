package model;

import io.vertx.core.json.JsonObject;

import java.util.Date;

/**
 * Created by sebastian markström on 2017-01-01.
 */
public class Message {
    private String body;
    private String date;
    private UserInfo sender;
    private UserInfo receiver;

    public Message() {
    }

    public Message(String body) {
        this.body = body;
        this.date = new Date().toString();
    }

    public Message (JsonObject json){
        this.body = json.getString("message");
        this.sender = new UserInfo(json.getJsonObject("receiver"));
        this.date = new Date().toString();
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject()
                .put("body", body)
                .put("date", date)
                .put("sender", sender.toJson())
                .put("receiver", receiver.toJson());
        return json;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public void setDate(Date date) {
        this.date = date.toString();
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
