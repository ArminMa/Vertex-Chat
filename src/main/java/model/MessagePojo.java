package model;

import io.vertx.core.json.JsonObject;

import java.util.Date;

/**
 * Created by sebastian markström on 2017-01-01.
 */
public class MessagePojo {
    private String message;
    private String groupid;
    private String date;
    private String sendername;

    public MessagePojo() {
    }

    public MessagePojo(String groupId, String message) {
        this.message = message;
        this.groupid = groupId;
    }

    public MessagePojo (JsonObject json){
        this.message = json.getString("message");
        this.groupid = json.getString("groupid");
        this.sendername = json.getString("sendername");
        if(json.getString("date") != null) {
            this.date = json.getString("date");
        } else {
            this.date = new Date().toString();
        }
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject()
                .put("message", message)
                .put("groupid", groupid)
                .put("sendername", sendername);
        if(date != null) {
            json.put("date", date);
        }
        return json;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSendername() {
        return sendername;
    }

    public void setSendername(String sendername) {
        this.sendername = sendername;
    }
}
