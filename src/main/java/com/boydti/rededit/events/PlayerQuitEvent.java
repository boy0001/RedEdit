package com.boydti.rededit.events;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.Event;

public class PlayerQuitEvent extends Event {
    private FawePlayer player;

    public PlayerQuitEvent() {}

    public void call(FawePlayer player) {
        this.player = player;
        WorldEdit.getInstance().getEventBus().post(this);
    }

    public FawePlayer getPlayer() {
        return player;
    }
}