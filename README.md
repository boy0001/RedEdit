# RedEdit [Java] [Minecraft]
Cross server code execution using redis and object serialization.

There are some teleportation commands bundled with it. (/tpa, /tpaccept, etc)
TODO document stuff.

## Example
```Java
// Create your task during startup
RemoteCall<Result, Argument> task = new RemoteCall<Result, Argument>() {
    @Override
    public String run(Server server, Integer arg) {
        // This task runs on the target server
        if (some condition) {
            // Return a result
            return some result;
        }
        // Or don't return a result
        return null;
    }
// You can set a custom serializer if you want
// Or it will default to java's object serializer
}.setSerializer(some serializer);

// A bit later
int serverGroup = 0; // Target all groups
int serverId = 0; // Target all servers in that group
Argument arg = <some argument>;

task.call(serverGroup, serverId, arg, new RunnableVal2<Server, String>() {
    @Override
    public void run(Server server, Result response) {
        // Do something with the result?
    }
});

// Other useful stuff
ServerController controller = RedEdit.get().getServerController();
PlayerListener playerListener = RedEdit.get().getPlayerListener();
RedUtil util = RedEdit.get().getUtil();
```
