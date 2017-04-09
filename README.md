# RedEdit [Java] [Minecraft]
The goal of this project is to abstract the network and simplify complex instructions.

RedEdit allows for cross server code execution
 - Uses Redis for communication
 - Supports Spigot/Bungee network - though can easily be ported to other platforms
 - Objects are serialized and compressed when transmitted
 
TODO:
 - Port to sponge/lily
 - Abstract connected players
 - Scripting support
 
## Example
Registering a remote call is easy; just create the object. The call can be received by any server which has it registered. 
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
```

You can then use call it whenever you want:
```Java
int serverGroup = 0; // Target all groups
int serverId = 0; // Target all servers in that group
Argument arg = <some argument>;

// Provide a runnable if you want to do something with the result.
task.call(serverGroup, serverId, arg, new RunnableVal2<Server, String>() {
    @Override
    public void run(Server server, Result response) {
        // Do something with the result or server?
    }
});
```
Misc classes
```Java
Network network = RedEdit.get().getNetwork(); // Get the connected players/servers
PlayerListener playerListener = RedEdit.get().getPlayerListener(); // Schedule a task on player join (timeout: 10s)
TeleportUtil util = RedEdit.get().getTeleportUtil(); // Various teleportation function
RedEdit.get().getUserConfig(uuid); // Get a player's homes
RedEdit.get().getWarpConfig(); // Get the warps
```
