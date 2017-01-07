package model;

import io.vertx.core.json.JsonObject;

/**
 * Created by sebastian markström on 2017-01-03.
 */
public class UserPojo {
    private String password;
    private String username;
    private String email;

    public UserPojo() {
    }

    public UserPojo(String password, String userName, String email) {
        this.password = password;
        this.username = userName;
        this.email = email;
    }

    public UserPojo(JsonObject json) {
        this.username = json.getString("username");
        this.password = json.getString("password");
        this.email = json.getString("email");
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("username", username)
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
