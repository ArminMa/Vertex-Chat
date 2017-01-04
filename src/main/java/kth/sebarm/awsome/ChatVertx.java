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
        System.out.println("start webapp called");
        // Create event bus
        EventBus eventBus = vertx.eventBus();

        // create routing indexes for 8090 port
        Router router = Router.router(vertx);
        router.route("/assets/*").handler(StaticHandler.create("assets"));
        router.post("/api/register");
        router.get("/api/users").handler(this::getUsers);
        router.get("/api/getMessages").handler(this::getAllMessagesFromUser);
        router.route().failureHandler(errorHandler());

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8082.
                        config().getInteger("http.port", 8090),
                        next::handle
                );
        //socket create group server
        createGroupServer();
        //create chat server
        createChatServer();
    }

    private void createGroupServer() {
        HttpServer server = vertx.createHttpServer();
        server.websocketHandler(webSocketHandler -> {
            System.out.println("Client connected");
            webSocketHandler.exceptionHandler(disconnectEvent -> {
                handleDisconnect(webSocketHandler);
            });

            webSocketHandler.closeHandler(closeEvent -> {
                connectedUsers.remove(webSocketHandler);
                System.out.println("socket closed");
            });

            webSocketHandler.handler(data -> {
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

                    //TODO check if group already exists
                    mongoClient.insert(GROUP_COLLECTION, group.toJson(), handler -> {
                       if (handler.succeeded()){
                           webSocketHandler.writeBinaryMessage(Buffer.buffer(String.valueOf(group.getId())));
                       } else {
                           webSocketHandler.writeBinaryMessage(Buffer.buffer("failed to create group :-C"));
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
            System.out.println("Client connected");
            webSocketHandler.exceptionHandler(disconnectEvent -> {
                //TODO
                System.out.println("Client socket disconnected");
                webSocketHandler.close();
            });
            webSocketHandler.closeHandler(closeEvent -> {
                //TODO
                System.out.println("user socket closed");
            });
            webSocketHandler.handler(data -> {
                System.out.println("Received data " + data.toString("ISO-8859-1"));
                MessagePojo message = Json.decodeValue(data.toString(), MessagePojo.class);
                System.out.println("parsed message: " + message.getMessage());

                Group group = groups.get(message.getGroupId());
                if(group == null){
                    group = new Group();
                    group.setId(message.getGroupId());
                    mongoClient.find(GROUP_COLLECTION, group.toJson(), handler -> {
                        if (handler.succeeded()) {
                            System.out.println("group found. attempting to parse data");
                            List<JsonObject> objects = handler.result();
                            Group newGroup = new Group(objects.get(0));
                            groups.put(newGroup.getId(), newGroup);
                            for (UserInfo user: newGroup.getUsers()) {
                                user.getUserSocket().writeBinaryMessage(Buffer.buffer(message.getMessage()));
                            }
                        } else {
                            System.out.println("Specified group is not registered. did not send message");
                            webSocketHandler.writeBinaryMessage(Buffer.buffer("Specified group is not registered. did not send message"));
                            webSocketHandler.close();
                        }
                    });
                }  else {
                    //write to all participants
                    for (UserInfo user: group.getUsers()) {
                        user.getUserSocket().writeBinaryMessage(Buffer.buffer(message.getMessage()));
                    }
                    System.out.println("message sent");
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

    private Group createGroup(UserInfo sender, String data) {
        System.out.println("in create group before parsing data from buffer");
        UserPojo[] userInfos = Json.decodeValue(data, UserPojo[].class);
        System.out.println("passed parse");
        List<UserInfo> users = new ArrayList<>();
        //create group
        Group group = new Group();
        for (UserPojo user : userInfos){
            for (UserInfo connectedUser : connectedUsers) {
                    if (connectedUser.getUserName().equals(user.getUserName()))
                    {
                        System.out.println("added user " + user.getUserName() + " to group");
                        users.add(connectedUser);
                        break;
                    }
            }
        }
        group.setUsers(users);
        group.addUser(sender);
        group.setId(group.generateId());
        System.out.println("group: " + group.toString());
        return group;
    }

    private static void handleDisconnect(ServerWebSocket webs) {
        System.out.println("client disconnected");
        webs.close();
        UserInfo user = getUserBySocketHandler(webs);
        connectedUsers.remove(user);
        System.out.println("usersockets size = " + connectedUsers.size());
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
}
