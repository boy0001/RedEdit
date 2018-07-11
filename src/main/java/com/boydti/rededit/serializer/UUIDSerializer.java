package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class UUIDSerializer implements Serializer<UUID> {
    @Override
    public void write(DataOutputStream dout, UUID value) throws IOException {
        dout.writeLong(value.getMostSignificantBits());
        dout.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public UUID read(DataInputStream din) throws IOException {
        return new UUID(din.readLong(), din.readLong());
    }
}
