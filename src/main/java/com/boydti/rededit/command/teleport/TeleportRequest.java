package com.boydti.rededit.command.teleport;

import com.boydti.rededit.serializer.Serializer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TeleportRequest {
    public final String sender;
    public final String receiver;
    public final boolean to;
    public final boolean override;
    public final long time;

    public TeleportRequest(String sender, String receiver, boolean to, boolean override) {
        this(sender, receiver, to, override, System.currentTimeMillis());
    }

    public TeleportRequest(String sender, String receiver, boolean to, boolean override, long time) {
        this.sender = sender;
        this.receiver = receiver;
        this.to = to;
        this.override = override;
        this.time = time;
    }

    public static Serializer<TeleportRequest> getSerializer() {
        return new Serializer<TeleportRequest>() {
            @Override
            public void write(DataOutputStream dout, TeleportRequest value) throws IOException {
                dout.writeUTF(value.sender);
                dout.writeUTF(value.receiver);
                dout.writeBoolean(value.to);
                dout.writeBoolean(value.override);
                dout.writeLong(value.time);
            }

            @Override
            public TeleportRequest read(DataInputStream din) throws IOException {
                String sender = din.readUTF();
                String receiver = din.readUTF();
                boolean to = din.readBoolean();
                boolean override = din.readBoolean();
                long time = din.readLong();
                return new TeleportRequest(sender, receiver, to, override, time);
            }
        };
    }
}