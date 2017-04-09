package com.boydti.rededit.listener;

import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.remote.Group;
import com.boydti.rededit.remote.Server;
import java.util.Collection;

public interface Network {
    Server getServer(int serverId);

    Group getGroup(int groupId);

    Collection<Server> getServers();

    Collection<Group> getGroups();

    Channel getServerChannel();

    void close();

    boolean isAlive(int groupId, int serverId);

    Collection<String> getPlayers(String startsWith);

    Server getServer(String player);

    default boolean isOnline(String player) {
        return getServer(player) != null;
    }

    default Server getLocalServer() {
        return getServer(Settings.IMP.SERVER_ID);
    }

    default Group getLocalGroup() {
        return getGroup(Settings.IMP.SERVER_GROUP);
    }
}
