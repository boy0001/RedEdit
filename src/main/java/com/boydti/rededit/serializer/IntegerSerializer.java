package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IntegerSerializer implements Serializer<Integer> {
    @Override
    public void write(DataOutputStream dout, Integer value) throws IOException {
        dout.writeInt(value);
    }

    @Override
    public Integer read(DataInputStream din) throws IOException {
        return din.readInt();
    }
}
