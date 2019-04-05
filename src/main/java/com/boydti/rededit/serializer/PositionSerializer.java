package com.boydti.rededit.serializer;

import com.boydti.rededit.remote.Position;
import com.sk89q.worldedit.math.Vector3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PositionSerializer implements Serializer<Position> {
    @Override
    public void write(DataOutputStream dout, Position value) throws IOException {
        String player = value.getPlayer();
        String world = value.getWorld();
        Vector3 pos = value.getPosition();
        Integer server = value.getServer();
        Integer group = value.getGroup();
        dout.writeBoolean(player != null); {
            dout.writeUTF(player);
        }
        dout.writeBoolean(world != null);
        if (world != null) {
            dout.writeUTF(world);
        }
        dout.writeBoolean(pos != null);
        if (pos != null) {
            dout.writeDouble(pos.getX());
            dout.writeDouble(pos.getY());
            dout.writeDouble(pos.getZ());
        }
        dout.writeBoolean(server != null);
        if (server != null) {
            dout.writeShort(server);
        }
        dout.writeBoolean(group != null);
        if (group != null) {
            dout.writeShort(group);
        }
    }

    @Override
    public Position read(DataInputStream din) throws IOException {
        String world;
        Vector3 pos;
        Integer server;
        Integer group;
        String player;
        if (din.readBoolean()) {
            player = din.readUTF();
        } else {
            player = null;
        }
        if (din.readBoolean()) {
            world = din.readUTF();
        } else {
            world = null;
        }
        if (din.readBoolean()) {
            pos = Vector3.at(din.readDouble(), din.readDouble(), din.readDouble());
        } else {
            pos = null;
        }
        if (din.readBoolean()) {
            server = (int) din.readShort();
        } else {
            server = null;
        }
        if (din.readBoolean()) {
            group = (int) din.readShort();
        } else {
            group = null;
        }
        return new Position(player, world, pos, server, group);
    }
}
