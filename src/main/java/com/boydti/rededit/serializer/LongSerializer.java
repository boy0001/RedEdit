package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LongSerializer implements Serializer<Long> {
    @Override
    public void write(DataOutputStream dout, Long value) throws IOException {
        dout.writeLong(value);
    }

    @Override
    public Long read(DataInputStream din) throws IOException {
        return din.readLong();
    }
}
