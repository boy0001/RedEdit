package com.boydti.rededit.serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EnumSerializer<T extends Enum> implements Serializer<T> {

    private final T[] values;

    public EnumSerializer(T[] values) {
        this.values = values;
    }

    @Override
    public void write(DataOutputStream dout, T value) throws IOException {
        dout.writeInt(value.ordinal());
    }

    @Override
    public T read(DataInputStream din) throws IOException {
        return values[din.readInt()];
    }
}
