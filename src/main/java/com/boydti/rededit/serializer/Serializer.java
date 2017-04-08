package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializer<T> {
    public void write(DataOutputStream dout, T value) throws IOException;

    public T read(DataInputStream din) throws IOException;
}
