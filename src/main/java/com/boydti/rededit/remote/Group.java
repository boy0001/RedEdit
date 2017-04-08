package com.boydti.rededit.remote;

import com.boydti.rededit.listener.ServerController;
import java.util.Collection;
import java.util.HashSet;

public class Group {
    private final Channel channel;
    private final ServerController reps;

    public Group(int id, ServerController reps) {
        this.channel = new Channel(id, 0);
        this.reps = reps;
    }

    public boolean isAlive() {
        return reps.isAlive(channel.getGroup(), 0);
    }

    public Collection<Server> getServers() {
        HashSet<Server> servers = new HashSet<>();
        int groupId = channel.getGroup();
        for (Server server : reps.getServers()) {
            if (server.getChannel().getGroup() == groupId) {
                servers.add(server);
            }
        }
        return servers;
    }

    public Channel getChannel() {
        return channel;
    }

    public ServerController getServerController() {
        return reps;
    }
}
