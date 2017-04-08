package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteArraySerializer implements Serializer<byte[]> {
    @Override
    public void write(DataOutputStream dout, byte[] value) throws IOException {
        dout.writeInt(value.length);
        dout.write(value);
    }

    @Override
    public byte[] read(DataInputStream din) throws IOException {
        byte[] bytes = new byte[din.readInt()];
        din.readFully(bytes);
        return bytes;
    }
}
