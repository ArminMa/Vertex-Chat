package model;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by sebastian markström on 2016-12-31.
 */
public class UserInfo implements Serializable, Comparator<UserInfo>, Comparable<UserInfo>{
    private ServerWebSocket userSocket;
    private String password;
    private String username;
    private String email;

    public UserInfo() {
    }

    public UserInfo(String email, String username, String password) {
        this.password = password;
        this.username = username;
        this.email = email;
    }

    public UserInfo(JsonObject json) {
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

    public ServerWebSocket getUserSocket() {
        return userSocket;
    }

    public void setUserSocket(ServerWebSocket userSocket) {
        this.userSocket = userSocket;
    }

    @Override
    public int compare(UserInfo o1, UserInfo o2) {
        return o1.getUsername().compareTo(o2.getUsername());
    }

    @Override
    public boolean equals(Object obj) {
        io.vertx.core.net.SocketAddress address = ((UserInfo)obj).userSocket.remoteAddress();
        System.out.println("address = " + address.toString());
        if(userSocket.remoteAddress().port() == address.port() && userSocket.remoteAddress().host().equals(address.host()))
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return userSocket.remoteAddress().port() + userSocket.remoteAddress().port();
    }

    @Override
    public int compareTo(UserInfo o) {
        return this.getUsername().compareTo(o.getUsername());
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
