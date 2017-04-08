package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BooleanSerializer implements Serializer<Boolean> {
    @Override
    public void write(DataOutputStream dout, Boolean value) throws IOException {
        dout.writeBoolean(value);
    }

    @Override
    public Boolean read(DataInputStream din) throws IOException {
        return din.readBoolean();
    }
}
