package com.boydti.rededit.listener;

import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.remote.RemoteCall;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4OutputStream;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedEditPubSub extends BinaryJedisPubSub {

    private JedisPool POOL;
    private final Map<Integer, Long> ALIVE_SERVERS;
    private final Map<Integer, Long> ALIVE_GROUPS;
    private final Map<Integer, Integer> SERVER_GROUP;
    private final Map<Integer, AtomicInteger> GROUP_COUNT;

    private final long GRACE_PERIOD_MS = 60000;
    private final long PING_INTERVAL = 30000;

    private final byte[] ALIVE_MESSAGE;
    private final byte[] DEAD_MESSAGE;

    private final Channel EVERYONE = new Channel(0, 0);
    private final Channel SERVER_CHANNEL;

    private ConcurrentHashMap<Integer, RemoteCall> FUNCTIONS = new ConcurrentHashMap<>();

    public RedEditPubSub(JedisPool pool) {
        this.POOL = pool;
        this.ALIVE_SERVERS = new ConcurrentHashMap<>();
        this.ALIVE_GROUPS = new ConcurrentHashMap<>();
        this.SERVER_GROUP = new ConcurrentHashMap<>();
        this.GROUP_COUNT = new ConcurrentHashMap<>();
        SERVER_CHANNEL = new Channel(Settings.IMP.SERVER_GROUP, Settings.IMP.SERVER_ID);
        ALIVE_MESSAGE = new byte[] {
                SERVER_CHANNEL.getId()[0],
                SERVER_CHANNEL.getId()[1],
                SERVER_CHANNEL.getId()[2],
                SERVER_CHANNEL.getId()[3],
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        DEAD_MESSAGE = new byte[] {
                SERVER_CHANNEL.getId()[0],
                SERVER_CHANNEL.getId()[1],
                SERVER_CHANNEL.getId()[2],
                SERVER_CHANNEL.getId()[3],
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1,
        };
        sendMessage(EVERYONE, ALIVE_MESSAGE);
        TaskManager.IMP.repeatAsync(new Runnable() {
            @Override
            public void run() {
                // Remove dead groups / servers
                sendMessage(EVERYONE, ALIVE_MESSAGE);
            }
        }, (int) (PING_INTERVAL / 50));
    }

    public Channel getServerChannel() {
        return SERVER_CHANNEL;
    }

    public void close() {
        sendMessage(EVERYONE, DEAD_MESSAGE);
        POOL = null;
    }

    @Override
    public void onMessage(byte[] channelBytes, byte[] message) {
        try {
            //* 4 = sender<br>
            //* 4 = method<br>
            //* 2 = sequence number<br>
            //* 1 = send or receive<br>
            //* # = data<br>
            FastByteArrayInputStream fbais = new FastByteArrayInputStream(message);
            LZ4InputStream stream = new LZ4InputStream(fbais, message.length);
            DataInputStream dataStream = new DataInputStream(stream);
            int group = dataStream.readShort();
            int server = dataStream.readShort();
            Channel channel = new Channel(group, server);
            int method = dataStream.readInt();
            if (method == 0) {
                int state = dataStream.read();
                switch (state) {
                    case 0:
                        setAlive(channel);
                        break;
                    case 1:
                        setDead(channel);
                        break;
                    default:
                        throw new UnsupportedOperationException("Invalid state: " + state);
                }
                return;
            }
            RemoteCall packet = FUNCTIONS.get(method);
            if (packet == null) {
                throw new UnsupportedOperationException("No protocol found for: " + method);
            }
            short sequence = dataStream.readShort();
            RemoteCall.Type type = RemoteCall.Type.values()[dataStream.read()];

            Object value = packet.readObject(dataStream, type);

            switch (type) {
                case RESULT: {
                    packet.result(channel, sequence, value);
                    break;
                }
                case ARGUMENT: {
                    packet.argument(channel, sequence, value);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public <T, V> RemoteCall<T, V> registerPacket(RemoteCall<T, V> packet) {
        if (FUNCTIONS.containsKey(packet.getId())) {
            throw new IllegalStateException("Packet with ID: " + packet.getId() + " is already registered!");
        }
        FUNCTIONS.put(packet.getId(), packet);
        return packet;
    }

    public DataOutputStream getOS(Channel channel) throws IOException {
        return getOS(channel.getId());
    }

    public DataOutputStream getOS(byte[] id) throws IOException {
        final FastByteArrayOutputStream fbaos = new FastByteArrayOutputStream();
        return new DataOutputStream(new LZ4OutputStream(fbaos, 1024) {
            private boolean closed = false;
            @Override
            public void close() throws IOException {
                super.close();
                if (closed != (closed = true)) {
                    sendMessageRaw(id, fbaos.toByteArray());
                }
            }
        });
    }

    private void sendMessage(Channel channel, byte[] message) {
        final FastByteArrayOutputStream fbaos = new FastByteArrayOutputStream();
        try {
            LZ4OutputStream os = new LZ4OutputStream(fbaos, Math.max(12, message.length));
            os.write(message);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        sendMessageRaw(channel, fbaos.toByteArray());
    }

    private void sendMessageRaw(Channel channel, byte[] message) {
        sendMessageRaw(channel.getId(), message);
    }

    private void sendMessageRaw(byte[] id, byte[] message) {
        Jedis jedis = POOL.getResource();
        try {
            jedis.publish(id, message);
        } catch (Exception e) {
            POOL.returnBrokenResource(jedis);
        } finally {
            POOL.returnResource(jedis);
        }
    }

    public boolean isAlive(int group, int server) {
        long now = System.currentTimeMillis();
        if (group != 0) {
            Long last = ALIVE_GROUPS.get(group);
            if (last == null || now - last > GRACE_PERIOD_MS) {
                return false;
            }
        }
        if (server != 0) {
            Long last = ALIVE_SERVERS.get(server);
            if (last == null || now - last > GRACE_PERIOD_MS) {
                return false;
            }
        }
        return true;
    }

    public void setAlive(Channel channel) {
        int group = channel.getGroup();
        int server = channel.getServer();
        this.ALIVE_SERVERS.put(server, System.currentTimeMillis());
        this.ALIVE_GROUPS.put(group, System.currentTimeMillis());
        if (this.SERVER_GROUP.put(server, group) == null) {
            AtomicInteger existing = GROUP_COUNT.get(group);
            if (existing == null) {
                GROUP_COUNT.put(group, new AtomicInteger(1));
            } else {
                existing.incrementAndGet();
            }
        }
    }

    public void setDead(Channel channel) {
        int group = channel.getGroup();
        int server = channel.getServer();
        this.ALIVE_SERVERS.remove(server);
        this.SERVER_GROUP.remove(server);
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Integer> entry : SERVER_GROUP.entrySet()) {
            if (entry.getValue() == group) {
                Long last = ALIVE_SERVERS.get(entry.getKey());
                if (last != null && now - last < GRACE_PERIOD_MS) {
                    return;
                }
            }
        }
        ALIVE_GROUPS.remove(group);
    }
}
