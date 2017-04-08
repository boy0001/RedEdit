package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DefaultSerializer implements Serializer<Object> {
    @Override
    public void write(DataOutputStream dout, Object value) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(dout)) {
            oos.writeObject(value);
        }
    }

    @Override
    public Object read(DataInputStream din) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(din)) {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
