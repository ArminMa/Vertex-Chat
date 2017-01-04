package model;

import io.vertx.core.json.JsonObject;

import java.util.Date;

/**
 * Created by sebastian markström on 2017-01-01.
 */
public class MessagePojo {
    private String message;
    private String groupId;
    private String date;

    public MessagePojo() {
    }

    public MessagePojo(String groupId, String message) {
        this.message = message;
        this.groupId = groupId;
    }

    public MessagePojo (JsonObject json){
        this.message = json.getString("message");
        this.groupId = json.getString("groupId");
        if(json.getString("date") != null) {
            this.date = json.getString("date");
        } else {
            this.date = new Date().toString();
        }
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject()
                .put("message", message)
                .put("groupId", groupId);
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
