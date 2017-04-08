package com.boydti.rededit.remote;

import com.boydti.fawe.object.Metadatable;
import com.boydti.rededit.listener.ServerController;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Metadatable {
    private final Channel channel;
    private final ServerController reps;
    private ConcurrentHashMap<String, Object> players = new ConcurrentHashMap<>();
    private Object present = new Object();

    public Server(Channel channel, ServerController reps) {
        this.channel = channel;
        this.reps = reps;
    }

    public Channel getChannel() {
        return channel;
    }

    public Group getGroup() {
        return reps.getGroup(getChannel().getGroup());
    }

    public int getId() {
        return channel.getServer();
    }

    public boolean isAlive() {
        return reps.isAlive(channel.getGroup(), channel.getServer());
    }

    public Collection<String> getPlayers() {
        return Collections.unmodifiableCollection(players.keySet());
    }

    public void addPlayer(String player) {
        this.players.put(player, present);
    }

    public void removePlayer(String player) {
        this.players.remove(player);
    }

    public boolean hasPlayer(String player) {
        return player.contains(player);
    }

    public ServerController getServerController() {
        return reps;
    }
}
