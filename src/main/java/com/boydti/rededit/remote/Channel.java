package com.boydti.rededit.remote;

public class Channel {
    public byte[] id;

    public Channel(int group, int server) {
        if ((char) group != group || (char) server != server) {
            throw new UnsupportedOperationException(group + "," + server + " is outside allowed range: (0," + (int) Character.MAX_VALUE + ")");
        }
        id = Channel.getId(group, server);
    }

    public Channel(byte[] id) {
        this.id = id;
    }

    public byte[] getId() {
        return id;
    }

    public int getGroup() {
        return (id[0] << 8) + id[1];
    }

    public int getServer() {
        return (id[2] << 8) + id[3];
    }

    @Override
    public String toString() {
        return getGroup() + ":" + getServer();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj == this || obj.hashCode() == hashCode());
    }

    @Override
    public int hashCode() {
        return (id[0] << 24) + (id[1] << 16) + (id[2] << 8) + (id[3]);
    }

    public static byte[] getId(int group, int server) {
        if ((char) group != group || (char) server != server) {
            throw new UnsupportedOperationException(group + "," + server + " is outside allowed range: (0," + (int) Character.MAX_VALUE + ")");
        }
        return new byte[]{(byte) (group >> 8), (byte) (group & 0xFF), (byte) (server >> 8), (byte) (server & 0xFF)};
    }
}
