package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ShortSerializer implements Serializer<Short> {
    @Override
    public void write(DataOutputStream dout, Short value) throws IOException {
        dout.writeShort(value);
    }

    @Override
    public Short read(DataInputStream din) throws IOException {
        return din.readShort();
    }
}
