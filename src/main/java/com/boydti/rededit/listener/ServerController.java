package com.boydti.rededit.listener;

import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.remote.Group;
import com.boydti.rededit.remote.Server;
import java.util.Collection;

public interface ServerController {
    Server getServer(int serverId);

    Group getGroup(int groupId);

    Collection<Server> getServers();

    Collection<Group> getGroups();

    Channel getServerChannel();

    void close();

    boolean isAlive(int groupId, int serverId);

    default boolean isOnline(String player) {
        return getServer(player) != null;
    }

    Collection<String> getPlayers(String startsWith);

    Server getServer(String player);
}
