package model;

import io.vertx.core.json.JsonObject;

/**
 * Created by sebastian markström on 2017-01-03.
 */
public class UserPojo {
    private String password;
    private String userName;
    private String email;

    public UserPojo() {
    }

    public UserPojo(String password, String userName, String email) {
        this.password = password;
        this.userName = userName;
        this.email = email;
    }

    public UserPojo(JsonObject json) {
        this.userName = json.getString("userName");
        this.password = json.getString("password");
        this.email = json.getString("email");
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("userName", userName)
                .put("password", password)
                .put("email", email);
        return json;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
