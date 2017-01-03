package model;

import io.vertx.core.json.JsonObject;

import java.util.Date;

/**
 * Created by sebastian markström on 2017-01-01.
 */
public class MessagePojo {
    public String message;
    public String receiverMail;
    public String senderMail;
    public String senderName;
    public String date;

    public MessagePojo() {
    }

    public MessagePojo(String receiver, String message) {
        this.message = message;
        this.receiverMail = receiver;
    }

    public MessagePojo (JsonObject json){
        this.message = json.getString("message");
        this.receiverMail = json.getString("receiver_mail");
        if(json.getString("date") != null) {
            this.date = json.getString("date");
        } else {
            this.date = new Date().toString();
        }
        if(json.getString("sender_mail") != null){
            this.senderMail = json.getString("sender_mail");
        }
        if(json.getString("sender_name") != null){
            this.senderMail = json.getString("sender_name");
        }
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject()
                .put("message", message)
                .put("receiver_mail", receiverMail);
        if(date != null) {
            json.put("date", date);
        }
        if(senderMail != null){
            json.put("sender_mail", senderMail);
        }
        if (senderName != null){
            json.put("sender_name", senderName);
        }
        return json;
    }
}
