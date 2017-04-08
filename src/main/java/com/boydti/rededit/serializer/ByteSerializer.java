package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteSerializer implements Serializer<Byte> {
    @Override
    public void write(DataOutputStream dout, Byte value) throws IOException {
        dout.writeByte(value);
    }

    @Override
    public Byte read(DataInputStream din) throws IOException {
        return din.readByte();
    }
}
