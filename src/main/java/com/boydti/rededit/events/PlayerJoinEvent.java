package com.boydti.rededit.events;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.Event;

public class PlayerJoinEvent extends Event {
    private Player player;

    public PlayerJoinEvent() {}

    public void call(Player player) {
        this.player = player;
        WorldEdit.getInstance().getEventBus().post(this);
    }

    public Player getPlayer() {
        return player;
    }
}