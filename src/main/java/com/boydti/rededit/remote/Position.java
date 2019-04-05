package com.boydti.rededit.remote;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import javax.annotation.Nullable;

public class Position {
    private final String world;
    private final Vector position;
    private final Integer server;
    private final String player;
    private final Integer group;

    public Position(String player, @Nullable String world, @Nullable Vector pos, @Nullable Integer server, @Nullable Integer group) {
        this.player = player;
        this.world = world;
        this.position = pos;
        this.server = server;
        this.group = group;
    }

    public String getPlayer() {
        return player;
    }

    public WorldVector getPosition(FawePlayer fp) {
        if (position == null) {
            return fp.getPlayer().getPosition();
        }
        if (world != null) {
            LocalWorld weWorld = (LocalWorld) FaweAPI.getWorld(world);
            if (weWorld != null) {
                return new WorldVector(weWorld, position);
            }
        }
        return new WorldVector((LocalWorld) fp.getWorld(), position);
    }

    @Nullable
    public Vector getPosition() {
        if (position != null) {
            if (world != null) {
                LocalWorld weWorld = (LocalWorld) FaweAPI.getWorld(world);
                if (weWorld != null) {
                    return new WorldVector(weWorld, position);
                }
            }
            return position;
        }
        return null;
    }

    @Override
    public String toString() {
        return world + "," + position.getX() + "," + position.getY() + "," + position.getZ() + "//" + server + "//" + group;
    }

    public String getWorld() {
        return world;
    }

    public Vector getVector() {
        return position;
    }

    @Nullable
    public Integer getServer() {
        return server;
    }

    @Nullable
    public Integer getGroup() {
        return group;
    }
}
