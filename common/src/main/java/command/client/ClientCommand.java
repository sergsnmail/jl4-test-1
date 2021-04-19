package command.client;

public enum ClientCommand {
    START("start"),
    END("end"),
    GET_FILE_INFO("get-file-info");

    private String commandName;

    ClientCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String toString() {
        return commandName;
    }

    public static ClientCommand getCommandByName(String commandName){
        if (commandName == null)
            throw new NullPointerException("Command name is null");

        for (ClientCommand command : ClientCommand.values()){
            if (commandName.equals(command.toString())){
                return command;
            }
        }

        throw new IllegalArgumentException("No enum constant " + ClientCommand.class.getCanonicalName() + " with command name " + commandName);
    }
}
