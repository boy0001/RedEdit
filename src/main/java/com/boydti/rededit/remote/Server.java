package com.boydti.rededit.remote;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.Metadatable;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.listener.Network;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Metadatable {
    private final Channel channel;
    private final Network reps;
    private String name;
    private ConcurrentHashMap<String, String> players = new ConcurrentHashMap<>();

    public Server(String name, Channel channel, Network reps) {
        this.name = name;
        this.channel = channel;
        this.reps = reps;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        return Collections.unmodifiableCollection(players.values());
    }

    public int getPlayerCount() {
        return players.size();
    }

    public void addPlayer(String player) {
        this.players.put(player.toLowerCase(), player);
    }

    public void removePlayer(String player) {
        this.players.remove(player.toLowerCase());
    }

    public boolean hasPlayer(String player) {
        return players.containsKey(player.toLowerCase());
    }

    public void teleportPlayer(FawePlayer player) {
        RedEdit.imp().teleport(player, getName());
    }

    public Network getServerController() {
        return reps;
    }

    public boolean isRemote() {
        return getId() != Settings.IMP.SERVER_ID;
    }
}
