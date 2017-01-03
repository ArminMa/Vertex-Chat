package kth.sebarm.awsome;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import model.MessagePojo;
import model.UserInfo;
import model.UserPojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebastian markström on 2017-01-02.
 */
public class ChatVertx extends AbstractVerticle{
    //General
    private static final Logger logger = LoggerFactory.getLogger(ChatVertx.class);
    private static List<UserInfo> connectedUsers = new ArrayList<>();
    private static EventBus eventBus;

    //Database
    private MongoClient mongoClient;
    private static final String USER_COLLECTION = "db_users";
    private static final String MESSAGE_COLLECTION ="db_messages";

    @Override
    public void start(Future<Void> fut) throws Exception {
        // Create a Mongo client
        mongoClient = MongoClient.createShared(vertx, config());
        createSomeData(
                (nothing) -> startWebApp(
                        (http) -> completeStartup(http, fut)
                ), fut);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public void startWebApp(Handler<AsyncResult<HttpServer>> next){
        System.out.println("start webapp called");
        // Create event bus
        eventBus = vertx.eventBus();
        // create routing indexes
        Router router = Router.router(vertx);
        router.route("/assets/*").handler(StaticHandler.create("assets"));
        router.get("/api/users").handler(this::getUsers);
        router.get("/api/getMessages").handler(this::getAllMessagesFromUser);
        router.route("/api/chat*").handler(BodyHandler.create());
        router.get("/api/chat/:id").handler(this::joinChatGroup);
        // /register
        router.route().failureHandler(errorHandler());

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8082.
                        config().getInteger("http.port", 8082),
                        next::handle
                );

        //socket server
        HttpServer server = vertx.createHttpServer();

        server.websocketHandler(webs -> {
            System.out.println("Client connected");
            webs.exceptionHandler(disconnectEvent -> {
                handleDisconnect(webs);
            });

            webs.closeHandler(closeEvent -> {
                connectedUsers.remove(webs);
                System.out.println("socket closed");
            });

            webs.handler(data -> {
                UserInfo sender = getUserBySocket(webs);
                if(sender == null) {
                    System.out.println("Received data " + data.toString("ISO-8859-1"));
                    addUserToList(webs, data);
                    System.out.println("connectedUsers size = " + connectedUsers.size());
                } else {
                    System.out.println("Received " + data.toString());
//                    sendMessage(data, sender);
                    //create group
                    List<UserInfo> users = Json.decodeValue("users", ArrayList.class);
                    System.out.println("list status " + users);
                    for (UserInfo user: users) {
                        System.out.println("user:" +user.toString());
                    }
                }
            });
        });

        server.listen(8083, "localhost", res -> {
            if (res.succeeded()) {
                System.out.println("Server is now listening!");
            } else {
                System.out.println("Failed to bind socketlistener!");
            }
        });
    }

    private void sendMessage(Buffer data, UserInfo sender) {
        MessagePojo messagePojo = new MessagePojo(data.toJsonObject());
        messagePojo.senderMail = sender.getEmail();
        messagePojo.senderName = sender.getUserName();
        String message = sender.getUserName() + ": " + messagePojo.message;

        for (UserInfo user : connectedUsers) {
            if (user.getEmail().equals(messagePojo.receiverMail)) {
                user.getUserSocket().writeBinaryMessage(Buffer.buffer().appendString(message));
            }
        }
        System.out.println("message " + message);
    }

    private static void handleDisconnect(ServerWebSocket webs) {
        System.out.println("client disconnected");
        webs.close();
        UserInfo user = getUserBySocket(webs);
        connectedUsers.remove(user);
        System.out.println("usersockets size = " + connectedUsers.size());
    }

    public void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut){
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
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
//        routingContext.response().end();
    }

    private void joinChatGroup(RoutingContext routingContext) {
        System.out.println("chat group called");
        int id = Integer.parseInt(routingContext.request().getParam("id"));
        System.out.println("group id:" + id);
        routingContext.response().end("chatgroup called. body: " + routingContext.request().toString());
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

    public void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> future){
        System.out.println("create some data called");
        List<UserPojo> users = new ArrayList<>();

        // add users if no users in database
        mongoClient.count(USER_COLLECTION, new JsonObject(), count -> {
            if (count.succeeded()) {
                if (count.result() == 0) {
                    UserPojo user = new UserPojo("user" , "user" , "user" );
                        mongoClient.insert(USER_COLLECTION, user.toJson(), ar -> {
                            if (ar.failed()) {
                                System.out.println("failed insert");
                                future.fail(ar.cause());
                            } else {
                                System.out.println("added user " + user.toJson());
                            }
                        });
                    }
                    next.handle(Future.<Void>succeededFuture());
                }
        });
    }

    private static UserInfo getUserBySocket(ServerWebSocket webs) {
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
}
