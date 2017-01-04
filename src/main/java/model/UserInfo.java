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
    private String userName;
    private String email;

    public UserInfo() {
    }

    public UserInfo(String email, String userName, String password) {
        this.password = password;
        this.userName = userName;
        this.email = email;
    }

    public UserInfo(JsonObject json) {
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

    public ServerWebSocket getUserSocket() {
        return userSocket;
    }

    public void setUserSocket(ServerWebSocket userSocket) {
        this.userSocket = userSocket;
    }

    @Override
    public int compare(UserInfo o1, UserInfo o2) {
        return o1.getUserName().compareTo(o2.getUserName());
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
        return this.getUserName().compareTo(o.getUserName());
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "password='" + password + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
