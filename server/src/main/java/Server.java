import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    private ExecutorService SERVICE = Executors.newFixedThreadPool(3);

    public static void main(String[] args) throws IOException {
        new Server().start();
    }

    private void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(9989);
        while (true) {
            try {
                LOGGER.info("Waiting accept client");
                Socket socket = serverSocket.accept();
                LOGGER.info("New client accepted");

                SERVICE.execute(new ClientHandler(socket));
            } catch (Exception e){
                LOGGER.error(e.getMessage());
            }
        }
    }

}
