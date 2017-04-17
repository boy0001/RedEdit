package com.boydti.rededit.remote;

import com.boydti.rededit.listener.Network;
import java.util.Collection;
import java.util.HashSet;

public class Group {
    private final Channel channel;
    private final Network reps;

    public Group(int id, Network reps) {
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

    public Server getSmallestServer() {
        Server min = null;
        int count = Integer.MAX_VALUE;
        for (Server server : getServers()) {
            int online = server.getPlayerCount();
            if (online < count) {
                min = server;
                count = online;
            }
        }
        return min;
    }

    public int getServerCount() {
        int total = 0;
        int groupId = channel.getGroup();
        for (Server server : reps.getServers()) {
            if (server.getChannel().getGroup() == groupId) {
                total++;
            }
        }
        return total;
    }

    public Channel getChannel() {
        return channel;
    }

    public Network getServerController() {
        return reps;
    }
}
