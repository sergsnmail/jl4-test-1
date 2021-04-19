package command.server;

import command.client.ClientCommand;

public enum ServerCommand {
    READY("ready"),
    GET_FILE_NAME("get-file-name"),
    FILE_NOT_EXIST("file-not-exist"),
    FILE_EXIST("file-exist"),
    FILE_METADATA_ERROR("file-metadata-error"),
    FILE_METADATA_READY("file-metadata-ready"),
    FILE_RECEIVED("file-received");


    private String commandName;

    ServerCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String toString() {
        return commandName;
    }

    public static ServerCommand getCommandByName(String commandName){
        if (commandName == null)
            throw new NullPointerException("Command name is null");

        for (ServerCommand command : ServerCommand.values()){
            if (commandName.equals(command.toString())){
                return command;
            }
        }

        throw new IllegalArgumentException("No enum constant " + ServerCommand.class.getCanonicalName() + " with command name " + commandName);
    }
}
