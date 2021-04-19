import java.io.*;
import java.net.Socket;

import command.client.ClientCommand;
import command.server.ServerCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);

    private final Socket socket;
    private static final String RECEIVED_DIR = "e:\\temp\\";
    private DataInputStream dIn;
    private DataOutputStream dOut;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dOut = new DataOutputStream(socket.getOutputStream())) {
            this.dIn = dIn;
            this.dOut = dOut;

            sendCommand(ServerCommand.READY);

            while (true) {
                try {
                    LOGGER.info( "Waiting message from client");
                    ClientCommand clCommand = waitClientCommand();
                    LOGGER.info( String.format("Received message from client %s", clCommand));
                    switch (clCommand) {
                        case START :
                            receiveFile();
                            break;
                        case GET_FILE_INFO:
                            getFileInfo();
                            break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Client disconnected with error: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void getFileInfo() throws IOException {
        sendCommand(ServerCommand.GET_FILE_NAME);
        String fileName = dIn.readUTF();
        if (fileName == null || fileName.isEmpty()) {
            throw new RuntimeException("Error received file name from client. The fileName string is null or empty!");
        }
        LOGGER.info(String.format("Received file name \'%s\'", fileName));
        File file = new File(RECEIVED_DIR + fileName);
        if (!file.exists()) {
            sendCommand(ServerCommand.FILE_NOT_EXIST);
        } else {
            sendCommand(ServerCommand.FILE_EXIST);
        }
    }

    private void receiveFile() throws IOException {
        String fName = dIn.readUTF();
        String fSubDir = dIn.readUTF();
        long fSize = dIn.readLong();

        if (fName == null || fName.isEmpty() || fSize == 0) {
            sendCommand(ServerCommand.FILE_METADATA_ERROR);
            throw new RuntimeException(String.format("Invalid received file metadata (fName %s, fSize %d)", fName, fSize));
        }

        LOGGER.info(String.format("File metadata received: file %s, total size %d\n", fName, fSize));

        if (!fSubDir.isEmpty()) {
            new File(RECEIVED_DIR + fSubDir).mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(RECEIVED_DIR + fSubDir + "\\" + fName)) {
            sendCommand(ServerCommand.FILE_METADATA_READY);

            byte[] buff = new byte[1024];
            long receive = 0;
            int count = 0;
            while (receive != fSize) {
                count = dIn.read(buff);
                if (count <= 0) {
                    LOGGER.error("Unexpected end of message while file receiving");
                    throw new RuntimeException("Unexpected end of message while file receiving");
                }
                fos.write(buff, 0, count);
                fos.flush();
                receive += count;
            }

            sendCommand(ServerCommand.FILE_RECEIVED);

            ClientCommand clCommand = waitClientCommand(); //ClientCommand.valueOf(dIn.readUTF());
            if (clCommand == ClientCommand.END) {
                LOGGER.info("Client send \'end\' marker. Received total %d bytes", receive);
            } else {
                LOGGER.error("Client not responding");
                throw new RuntimeException();
            }
        }
    }

    private void sendCommand(ServerCommand command) throws IOException {
        dOut.writeUTF(command.toString());
        dOut.flush();
        LOGGER.info("Send command \'%s\'", command);
    }

    private ClientCommand waitClientCommand(){
        String inputCommand = "";
        try {
            inputCommand = dIn.readUTF();
            ClientCommand clCommand = ClientCommand.getCommandByName(inputCommand);
            return clCommand;
        }catch (Exception e){
            LOGGER.error("Unknown command from client: %s", inputCommand);
        }
        throw new RuntimeException(String.format("Unknown command from client: %s", inputCommand));
    }
}
