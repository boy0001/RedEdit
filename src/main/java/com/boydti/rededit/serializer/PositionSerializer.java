package com.boydti.rededit.serializer;

import com.boydti.rededit.remote.Position;
import com.sk89q.worldedit.Vector;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PositionSerializer implements Serializer<Position> {
    @Override
    public void write(DataOutputStream dout, Position value) throws IOException {
        String player = value.getPlayer();
        String world = value.getWorld();
        Vector pos = value.getPosition();
        Integer server = value.getServer();
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
    }

    @Override
    public Position read(DataInputStream din) throws IOException {
        String world;
        Vector pos;
        Integer server;
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
            pos = new Vector(din.readDouble(), din.readDouble(), din.readDouble());
        } else {
            pos = null;
        }
        if (din.readBoolean()) {
            server = (int) din.readShort();
        } else {
            server = null;
        }
        return new Position(player, world, pos, server);
    }
}
