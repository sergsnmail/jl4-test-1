import command.client.ClientCommand;
import command.server.ServerCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Client {

    private static final Logger LOGGER = LogManager.getLogger(Client.class);

    private static final String OBSERVER_DIR = "d:\\FOTO\\passp";

    private HashSet<File> fileHashSet = new HashSet<>();
    private DataOutputStream dOut;
    private DataInputStream dIn;
    private String observerDir;

    public Client(String s) {
        this.observerDir = s;
    }

    public static void main(String[] args) {
        new Client(Client.OBSERVER_DIR).start();
    }

    private void start() {
        while (true) {
            try (Socket socket = new Socket("localhost", 9989);
                 OutputStream out = new BufferedOutputStream(socket.getOutputStream());
                 InputStream in = new BufferedInputStream(socket.getInputStream())) {
                dOut = new DataOutputStream(out);
                dIn = new DataInputStream(in);

                ServerCommand sCommand = waitServerCommand(); //ServerCommand.getCommandByName(dIn.readUTF());
                if (sCommand == ServerCommand.READY) {
                    File dir = new File(observerDir);
                    if (!dir.exists() && !dir.isDirectory()) {
                        LOGGER.error(String.format("%s not exist or not directory", observerDir));
                        throw new RuntimeException(String.format("%s not exist or not directory", observerDir));
                    }
                    while (true) {
                        collectFiles(dir);
                        if (fileHashSet.size() > 0) {
                            for (File file : fileHashSet) {
                                if (!fileExistOnServer(file)) {
                                    sendFile(file);
                                }
                            }
                        }
                        Thread.sleep(5000);
                    }
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Server not available");
            }
        }
    }

    private void collectFiles(File dir) {
        List<File> filesInDir = new ArrayList<>(Arrays.asList(dir.listFiles()));
        for (File file : filesInDir) {
            if (file.isDirectory()) {
                collectFiles(file);
            } else {
                fileHashSet.add(file);
            }
        }
    }

    private boolean fileExistOnServer(File file) throws IOException {
        sendCommand(ClientCommand.GET_FILE_INFO);

        ServerCommand sCommand = waitServerCommand(); //ServerCommand.getCommandByName(dIn.readUTF());
        if (sCommand == ServerCommand.GET_FILE_NAME){
            String fName = file.getName();
            String fSubDir = file.getAbsolutePath().replace(observerDir + "\\", "").replace(fName, "");
            sendMessage(fSubDir + fName);

            sCommand = waitServerCommand(); //ServerCommand.getCommandByName(dIn.readUTF());
            if(sCommand == ServerCommand.FILE_NOT_EXIST){
                return false;
            }
        }
        return true;
    }

    private void sendFile(File file) throws IOException {
        String fName = file.getName();
        String fSubDir = file.getAbsolutePath().replace(observerDir + "\\", "").replace(fName, "");
        long fSize = file.length();

        sendCommand(ClientCommand.START);
        sendMetadata(fName, fSubDir, fSize);

        ServerCommand srvCommand = waitServerCommand();
        if (ServerCommand.FILE_METADATA_READY.equals(srvCommand)) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                int count;
                byte[] buff = new byte[1024];
                while ((count = is.read(buff)) != -1) {
                    dOut.write(buff, 0, count);
                    dOut.flush();
                }
            }

            srvCommand = waitServerCommand();
            if (ServerCommand.FILE_RECEIVED.equals(srvCommand)) {
                sendCommand(ClientCommand.END);
            }
        }
        if (ServerCommand.FILE_METADATA_ERROR.equals(srvCommand)){
            System.out.printf("File sending error (fName %s, fSubDir %s, fSize %d)\n", fName, fSubDir, fSize);
        }
    }

    private void sendMetadata(String fName, String fSubDir, long fSize) throws IOException {
        sendMessage(fName);
        sendMessage(fSubDir);
        sendMessage(fSize);
    }

    private void sendMessage(String message) throws IOException {
        dOut.writeUTF(message);
        dOut.flush();
    }

    private void sendMessage(long message) throws IOException {
        dOut.writeLong(message);
        dOut.flush();
    }

    private void sendCommand(ClientCommand clCommand) throws IOException {
        dOut.writeUTF(clCommand.toString());
        dOut.flush();
    }

    private ServerCommand waitServerCommand(){
        String inputCommand = "";
        try {
            inputCommand = dIn.readUTF();
            ServerCommand sCommand = ServerCommand.getCommandByName(inputCommand);
            return sCommand;
        }catch (Exception e){
            LOGGER.error("Unknown command from server: %s", inputCommand);
        }
        throw new RuntimeException(String.format("Unknown command from server: %s", inputCommand));
    }
}
