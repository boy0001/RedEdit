package com.boydti.rededit.serializer;

import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class TagSerializer implements Serializer<Tag> {
    @Override
    public void write(DataOutputStream dout, Tag value) throws IOException {
        new NBTOutputStream((DataOutput) dout).writeNamedTag("", value);
    }

    @Override
    public Tag read(DataInputStream din) throws IOException {
        return new NBTInputStream((DataInput) din).readNamedTag().getTag();
    }
}
