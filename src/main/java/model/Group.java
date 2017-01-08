package model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * Created by sebastian markström on 2017-01-02.
 */
public class Group {
    private String _id = "";
    private List<UserInfo> users;

    public Group() {
        users = new ArrayList<>();
    }

    public Group(List<UserInfo> users) {
        this.users = users;
        Collections.sort(this.users);
        _id = generateId();
    }

    public Group(UserInfo[] users) {
        this.users = Arrays.asList(users);
        Collections.sort(this.users);
        _id = generateId();
    }

    public Group(JsonObject json){
        System.out.println("in group constructor");
        this._id = json.getString("id");
        this.users = new ArrayList<>();
        JsonArray users = json.getJsonArray("users");
        for (int i = 0; i < users.size(); i++){
            this.users.add(new UserInfo(users.getJsonObject(i)));
        }
        System.out.println("outside group constructor");
    }

    public JsonObject toJson(){
        JsonArray list = new JsonArray();
        for (UserInfo user: users) {
            list.add(user.toJson());
        }
        JsonObject json = new JsonObject()
                .put("id", _id)
                .put("users", list);
        return json;
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void addUsers(List<UserInfo> users){
        this.users.addAll(users);
        Collections.sort(this.users);
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
        Collections.sort(this.users);
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public void addUser(UserInfo user) {
        users.add(user);
        Collections.sort(this.users);
    }

    public void removeUser(UserInfo userInfo) {
        users.remove(userInfo);
    }

    /**
     * Generates an id based on participating users
     * @return id
     */
    public String generateId(){
        String id = "";
        for (UserInfo user: users) {
            id += user.getUsername();
        }
        return id;
    }

    public static Comparator<Group> comparator = new Comparator<Group>() {
        @Override
        public int compare(Group group1, Group group2) {
            return (group1.getId().compareTo(group2.getId()));
        }
    };

    public static Comparator<Group> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id='" + _id + '\'' +
                ", users=" + users +
                '}';
    }
}
