package kth.sebarm.awsome;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import model.MessagePojo;
import model.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class clientTest {
    static String message;
    static final HttpClient client = Vertx.vertx().createHttpClient();
    static WebSocket socket;

    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        UserInfo user = createAUser(userInput);
        System.out.println("created user in json:" + System.lineSeparator() + user.toJson().toString());
        createSocket(user.toJson().toString());
        List<JsonObject> usersJson = new ArrayList<>();

        while (true) {
            usersJson.add(createAJsonUser(userInput));
            socket.writeBinaryMessage(Buffer.buffer(usersJson.toString()));
        }
    }

    private static void sendMessage(Scanner userInput) {
        System.out.println("enter receiver");
        String receiver = userInput.nextLine();
        System.out.println("enter message");
        sendMessage(receiver, userInput.nextLine());
    }

    private static UserInfo createAUser(Scanner userInput) {
        System.out.println("enter email");
        String email = userInput.nextLine();
        System.out.println("enter username");
        String userName = userInput.nextLine();
        System.out.println("enter password");
        String password = userInput.nextLine();
        return new UserInfo(email, userName, password);
    }

    private static JsonObject createAJsonUser(Scanner userInput) {
        System.out.println("enter email");
        String email = userInput.nextLine();
        System.out.println("enter username");
        String userName = userInput.nextLine();
        System.out.println("enter password");
        String password = userInput.nextLine();
        return new UserInfo(email, userName, password).toJson();
    }

    private static void createSocket(String loginInfo) {
        client.websocket(8083, "localhost", "/some-uri", webSocket ->
        {
            socket = webSocket;
            webSocket.handler(data ->
            {
                System.out.println(data.toString());
            });
            webSocket.writeBinaryMessage(Buffer.buffer(loginInfo));
        });
    }

    private static void sendMessage(String receiver, String message){
        MessagePojo messagePojo = new MessagePojo(receiver, message);
        System.out.println("Messagepojo as json: " + System.lineSeparator() + messagePojo.toJson().toString());
        socket.writeBinaryMessage(Buffer.buffer(messagePojo.toJson().toString()));
    }

}