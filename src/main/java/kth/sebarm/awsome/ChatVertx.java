package kth.sebarm.awsome;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebastian markström on 2017-01-02.
 */
public class ChatVertx extends AbstractVerticle{
    //General
    private static final Logger logger = LoggerFactory.getLogger(ChatVertx.class);
    private static List<UserInfo> connectedUsers = new ArrayList<>();
    private static HashMap<String, Group> groups = new HashMap<>();

    //Database
    private MongoClient mongoClient;
    private static final String USER_COLLECTION = "db_users";
    private static final String MESSAGE_COLLECTION ="db_messages";
    private static final String GROUP_COLLECTION = "db_groups";

    @Override
    public void start(Future<Void> future) throws Exception {
        // Create a Mongo client
        mongoClient = MongoClient.createShared(vertx, config());
        createSomeData(
                (nothing) -> startWebApp(
                        (http) -> completeStartup(http, future)
                ), future);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public void startWebApp(Handler<AsyncResult<HttpServer>> next){
        //socket create group server
        createGroupServer();
        //create chat server
        createChatServer();
    }

    private void createGroupServer() {
        HttpServer server = vertx.createHttpServer();
        server.websocketHandler(webSocketHandler -> {
            System.out.println("Client connected to register/group server");
            webSocketHandler.exceptionHandler(disconnectEvent -> {
                logger.info(webSocketHandler.remoteAddress().toString() + ", disconnected");
                handleDisconnect(webSocketHandler);
            });

            webSocketHandler.closeHandler(closeEvent -> {
                System.out.println("socket closed");
                handleDisconnect(webSocketHandler);
            });

            webSocketHandler.handler(data -> {
                logger.info("received data from client: " + data.toString());
                UserInfo sender = getUserBySocketHandler(webSocketHandler);
                if(sender == null) {
                    System.out.println("Received data " + data.toString("ISO-8859-1"));
                    addUserToList(webSocketHandler, data);
                    System.out.println("connectedUsers size = " + connectedUsers.size());
                }
                //create group
                else {
                    System.out.println("Received " + data.toString());
                    Group group = createGroup(sender, data.toString());
                    logger.info("group created");
                    // save the group in the MongoDB
                    mongoClient.insert(GROUP_COLLECTION, group.toJson(), handler -> {
                        if (handler.succeeded()){
                            logger.info("group created");
                            //mongodb generated _id
                            String groupId = handler.result();
                            logger.info("group id = " + groupId);
                            JsonObject json = new JsonObject();
                            json.put("groupid", groupId);
                            logger.info("sent message:" + json.toString());
                            for (UserInfo user: group.getUsers()) {
                                logger.info(user.getUsername());
                                user.getUserSocket().writeFinalTextFrame(json.toString());
                            }
                        } else {
                            logger.info("something went wrong. group not created");
                            webSocketHandler.writeFinalTextFrame("failed to create group :-C");
                        }
                    });
                }
            });
        }).listen(config().getInteger("socket.group.port", 5091), "localhost", res -> {
            if (res.succeeded()) {
                System.out.println("Group server is now listening!");
            } else {
                System.out.println("Failed to bind group server socket listener!");
            }
        });
    }

    private void createChatServer() {
        HttpServer chatServer = vertx.createHttpServer();
        chatServer.websocketHandler(webSocketHandler -> {
            System.out.println("Client connected to chatserver");
            webSocketHandler.exceptionHandler(disconnectEvent -> {
                //TODO
                System.out.println("Client socket disconnected");
                webSocketHandler.close();
            });
            webSocketHandler.closeHandler(closeEvent -> {
                //TODO
                System.out.println("user socket closed");
                handleChatServerDisconnect(webSocketHandler);
            });
            webSocketHandler.handler(data -> {
                System.out.println("Received data " + data.toString("ISO-8859-1"));
                MessagePojo message = Json.decodeValue(data.toString(), MessagePojo.class);
                Group group = groups.get(message.getGroupid());

                //if group not in memory. get group from db
                if(group == null){
                    logger.info("group not in memory. fetching dgroup from database");
                    JsonObject query = new JsonObject().put("_id", message.getGroupid());
                    logger.info("search group by id: " + message.getGroupid());
                    mongoClient.find(GROUP_COLLECTION, query, handler -> {
                        if (handler.succeeded()) {
                            System.out.println(handler.result().size() + " groups found. attempting to parse data");
                            List<JsonObject> objects = handler.result();
                            Group newGroup = new Group(objects.get(0));
                            newGroup.setId(message.getGroupid());
                            addSocketTooGroup(newGroup, webSocketHandler, message.getSendername());
                            groups.put(message.getGroupid(), newGroup);
                        } else {
                            System.out.println("Specified group is not registered. did not send message");
                            webSocketHandler.writeFinalTextFrame("Specified group is not registered. did not send message");
                            webSocketHandler.close();
                        }
                    });
                }  else {
                    logger.info("group already in memory. proceeding normally");
                    addUserSocketIfNotRegistered(webSocketHandler, message, group);
                    if(message.getMessage() != null && !message.getMessage().equals("")) {
                        String completeMessage = message.getSendername() + ": " + message.getMessage();
                        sendMessageToGroup(group, completeMessage);
                        logger.info("message sent to all connected users");
                    } else {
                        logger.info("user registered");
                    }
                }
            });
        }).listen(config().getInteger("socket.chat.port", 5092), "localhost", res -> {
            if (res.succeeded()) {
                System.out.println("chat server is now listening!");
            } else {
                System.out.println("Failed to bind chat server socket!");
            }
        });
    }

    private void handleChatServerDisconnect(ServerWebSocket webSocketHandler) {
        Group removeGroup = null;
        UserInfo removedUser = null;
        for (Group group : groups.values()) {
            logger.info("itterating through groups");
            for (UserInfo user: group.getUsers()) {
                if(user.getUserSocket() != null) {
                    if (user.getUserSocket().equals(webSocketHandler)) {
                        removeGroup = group;
                        removedUser = user;
                        user.setUserSocket(null);
                    }
                }
            }
        }

        for (UserInfo user: removeGroup.getUsers()){
            if (user.getUserSocket() != null){
                logger.info("users are still online. group still in memory");
                String message = removedUser.getUsername() + " has left the chat";
                sendMessageToGroup(removeGroup, message);
                return;
            }
        }
        logger.info("all participants from group has left. removing group from memory");
        groups.remove(removeGroup.getId());
        logger.info("number of online groups is " + groups.size());
    }

    /**
     * Sends a message to all connected users
     */
    private void sendMessageToGroup(Group group, String message) {
        for (UserInfo userInfo: group.getUsers()) {
            if (userInfo.getUserSocket() != null){
                userInfo.getUserSocket().writeFinalTextFrame(message);
            }
        }
    }

    private void addUserSocketIfNotRegistered(ServerWebSocket webSocketHandler, MessagePojo message, Group group) {
        for (UserInfo user: group.getUsers()) {
            if(user.getUsername().equals(message.getSendername())){
                if(user.getUserSocket() == null){
                    user.setUserSocket(webSocketHandler);
                }
                break;
            }
        }
    }

    private void addSocketTooGroup(Group newGroup, ServerWebSocket webSocketHandler, String username) {
        logger.info("in add socket to group chatter");
        for (int i = 0; i < newGroup.getUsers().size(); i++) {
            if (newGroup.getUsers().get(i).getUsername().equals(username)){
                newGroup.getUsers().get(i).setUserSocket(webSocketHandler);
            }
        }
    }

    private Group createGroup(UserInfo sender, String data) {
        System.out.println("in create group before parsing data from buffer");
        UserPojo userInfo = Json.decodeValue(data, UserPojo.class);
        System.out.println("passed parse");
        for (UserInfo connectedUser : connectedUsers) {
            if(userInfo.getUsername().equals(connectedUser.getUsername())){
                Group group = new Group();
                List<UserInfo> users = new ArrayList<UserInfo>();
                users.add(connectedUser);
                users.add(sender);
                group.setUsers(users);
                logger.info("created group");
                return group;
            }
        }
        return null;
    }

    private static void handleDisconnect(ServerWebSocket webs) {
        logger.info("in handle disconnect");
        UserInfo user = getUserBySocketHandler(webs);
        connectedUsers.remove(user);
        System.out.println("usersockets size = " + connectedUsers.size());
        if(webs != null)
            webs.close();
    }

    public void completeStartup(AsyncResult<HttpServer> http, Future<Void> future){
        if (http.succeeded()) {
            future.complete();
        } else {
            future.fail(http.cause());
        }
    }

    private void getUsers(RoutingContext routingContext) {
        System.out.println("get connectedUsers called");
        mongoClient.find(USER_COLLECTION, new JsonObject(), results -> {
            List<JsonObject> objects = results.result();
            System.out.println("objects size = " + objects.size());
            List<UserPojo> users = new ArrayList<>(objects.size());
            for (JsonObject object: objects) {
                System.out.println("object content: " + object.toString());
                users.add(new UserPojo(object));
            }
            for (UserPojo user: users) {
                System.out.println("user:" + user.toJson());
            }
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(users));
            System.out.println("response sent");
        });
    }

    private void getAllMessagesFromUser(RoutingContext routingContext) {
        System.out.println("get all messages from user called");
//        routingContext.response().end("get all messages from user called. body sent: " + routingContext.getBodyAsJson());
    }

    private static void addUserToList(ServerWebSocket webs, Buffer data) {
        UserInfo user = new UserInfo(data.toJsonObject());
        user.setUserSocket(webs);
        connectedUsers.add(user);
    }

    private void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> future){
        System.out.println("create some data called");
        List<UserPojo> users = new ArrayList<>();

        // add users if no users in database
        mongoClient.count(USER_COLLECTION, new JsonObject(), count -> {
            if (count.succeeded()) {
                if (count.result() == 0) {
                    UserPojo user = new UserPojo("user" , "user" , "user" );
                    UserPojo user2 = new UserPojo("user2" , "user2" , "user2" );
                    mongoClient.insert(USER_COLLECTION, user.toJson(), ar -> {
                        if (ar.failed()) {
                            System.out.println("failed insert");
                            future.fail(ar.cause());
                        } else {
                            mongoClient.insert(USER_COLLECTION, user.toJson(), result -> {
                                if(result.failed()){
                                    System.out.println("second user failed to insert");
                                } else {
                                    System.out.println("success second user added");
                                }
                            });
                            System.out.println("added user " + user.toJson());
                            System.out.println("and user " + user2.toJson());
                        }
                    });
                }
                System.out.println("users already exist. Wont add users");
                next.handle(Future.<Void>succeededFuture());
            }
        });
    }

    private static UserInfo getUserBySocketHandler(ServerWebSocket webs) {
        System.out.println("incomming socket: " + webs.remoteAddress().toString());
        for (UserInfo user: connectedUsers) {
            System.out.println("compared to address and port: " + user.getUserSocket().remoteAddress().toString());
            if(user.getUserSocket().remoteAddress().port() == webs.remoteAddress().port() &&
                    user.getUserSocket().remoteAddress().host().equals(webs.remoteAddress().host()))
            {
                return user;
            }
        }
        return null;
    }

    private ErrorHandler errorHandler() {
        return ErrorHandler.create();
    }

    //dynamic socket creation
    //create socket server receiver and then notify users
//                    WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool");
//                    executor.executeBlocking(future -> {
//                        System.out.println("in serial");
//                        createPrivateChatRoom(group);
//                        future.complete();
//                    }, res -> {
//                        //notify users
//                        System.out.println("sending message to users in group");
//                        sendMessageToGroup(data, sender, group);
//                    });

    //create a private chat room
//    private void createPrivateChatRoom(Group onlineGroup) {
//        HttpServer server = vertx.createHttpServer();
//        server.websocketHandler(webSocketHandler -> {
//            System.out.println("Client connected to group" + onlineGroup.getId());
//            webSocketHandler.exceptionHandler(disconnectEvent -> {
//                webSocketHandler.close();
//                onlineGroup.removeUser(getUserBySocketHandler(webSocketHandler));
//                System.out.println("client disconnected. removed from group");
//                if (onlineGroup.getUsers().size() == 0){
//                    groups.remove(onlineGroup);
//                    server.close();
//                }
//            });
//
//            webSocketHandler.closeHandler(closeEvent -> {
//                onlineGroup.removeUser(getUserBySocketHandler(webSocketHandler));
//                System.out.println("socket closed");
//                if (onlineGroup.getUsers().size() == 0){
//                    groups.remove(onlineGroup);
//                    server.close();
//                }
//            });
//
//            webSocketHandler.handler(data -> {
//                UserInfo sender = getUserByGroup(webSocketHandler, onlineGroup);
//                if(sender == null){
//                    System.out.println("sender was not found in group. aborting message");
//                    return;
//                }
//                sendMessageToGroup(data, sender, onlineGroup);
//            });
//        });
//        server.listen(config().getInteger("socket.chat.port", 5092));
//    }

    //  private Group createGroup(UserInfo sender, String data) {
//        System.out.println("in create group before parsing data from buffer");
//        UserPojo[] userInfos = Json.decodeValue(data, UserPojo[].class);
//        System.out.println("passed parse");
//        List<UserInfo> users = new ArrayList<>();
//        //create group
//        Group group = new Group();
//        for (UserPojo user : userInfos){
//            for (UserInfo connectedUser : connectedUsers) {
//                    if (connectedUser.getUsername().equals(user.getUsername()))
//                    {
//                        System.out.println("added user " + user.getUsername() + " to group");
//                        users.add(connectedUser);
//                        break;
//                    }
//            }
//        }
//        group.setUsers(users);
//        group.addUser(sender);
//        group.setId(group.generateId());
//        System.out.println("group: " + group.toString());
//        return group;
//    }
}
