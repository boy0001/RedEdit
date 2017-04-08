package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StringSerializer implements Serializer<String> {
    @Override
    public void write(DataOutputStream dout, String value) throws IOException {
        dout.writeUTF(value);
    }

    @Override
    public String read(DataInputStream din) throws IOException {
        return din.readUTF();
    }
}
