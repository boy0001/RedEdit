package com.boydti.rededit.remote;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;

public class Position {
    private final String world;
    private final Vector3 position;
    private final Integer server;
    private final String player;
    private final Integer group;

    public Position(String player, @Nullable String world, @Nullable Vector3 pos, @Nullable Integer server, @Nullable Integer group) {
        this.player = player;
        this.world = world;
        this.position = pos;
        this.server = server;
        this.group = group;
    }

    public String getPlayer() {
        return player;
    }

    public Location getPosition(FawePlayer fp) {
        if (position == null) {
            return fp.getPlayer().getLocation();
        }
        if (world != null) {
            World weWorld = FaweAPI.getWorld(world);
            if (weWorld != null) {
                return new Location(weWorld, position);
            }
        }
        return new Location(fp.getWorld(), position);
    }

    @Nullable
    public Vector3 getPosition() {
        if (position != null) {
            if (world != null) {
                World weWorld = FaweAPI.getWorld(world);
                if (weWorld != null) {
                    return new Location(weWorld, position);
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

    public Vector3 getVector() {
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
