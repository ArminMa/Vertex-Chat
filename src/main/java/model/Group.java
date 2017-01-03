package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by sebastian markström on 2017-01-02.
 */
public class Group {
    private long id;
    private List<UserInfo> users;

    public Group(long id, List<UserInfo> users) {
        this.id = id;
        this.users = users;
    }

    public Group(long id,UserInfo[] users) {
        this.id = id;
        this.users = Arrays.asList(users);
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void addUsers(List<UserInfo> users){
        this.users.addAll(users);
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
