package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoidSerializer implements Serializer<Object> {
    @Override
    public void write(DataOutputStream dout, Object value) throws IOException {

    }

    @Override
    public Object read(DataInputStream din) throws IOException {
        return new Object();
    }
}
