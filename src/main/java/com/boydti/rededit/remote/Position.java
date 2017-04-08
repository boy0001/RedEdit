package com.boydti.rededit.remote;

import com.sk89q.worldedit.WorldVector;
import javax.annotation.Nullable;

public class Position {
    private final WorldVector position;
    private final Integer server;

    public Position(@Nullable WorldVector pos, @Nullable Integer server) {
        this.position = pos;
        this.server = server;
    }

    @Nullable
    public WorldVector getPosition() {
        return position;
    }

    @Nullable
    public Integer getServer() {
        return server;
    }
}
