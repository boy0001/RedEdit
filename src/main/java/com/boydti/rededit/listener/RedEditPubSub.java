package com.boydti.rededit.listener;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.events.ServerStartEvent;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.remote.Group;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.util.MapUtil;
import com.google.common.cache.LoadingCache;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4OutputStream;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedEditPubSub extends BinaryJedisPubSub implements Network {

    private JedisPool POOL;
    private final LoadingCache<Integer, Server> ALIVE_SERVERS;
    private final LoadingCache<Integer, Group> ALIVE_GROUPS;

    private ServerStartEvent serverStart;

    private final long GRACE_PERIOD_MS = 60000;
    private final long PING_INTERVAL_MS = 30000;

    private final byte[] START_MESSAGE;
    private final byte[] ALIVE_MESSAGE;
    private final byte[] DEAD_MESSAGE;

    private final Channel EVERYONE = new Channel(0, 0);
    private final Channel SERVER_CHANNEL;

    private ConcurrentHashMap<Integer, RemoteCall> FUNCTIONS = new ConcurrentHashMap<>();
    private AtomicBoolean started = new AtomicBoolean();

    public RedEditPubSub(JedisPool pool) throws IOException {
        this.POOL = pool;
        this.ALIVE_SERVERS = MapUtil.getExpiringMap(GRACE_PERIOD_MS, TimeUnit.MILLISECONDS);
        this.ALIVE_GROUPS = MapUtil.getExpiringMap(GRACE_PERIOD_MS, TimeUnit.MILLISECONDS);
        SERVER_CHANNEL = new Channel(Settings.IMP.SERVER_GROUP, Settings.IMP.SERVER_ID);
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(SERVER_CHANNEL.getGroup());
            dos.writeShort(SERVER_CHANNEL.getServer());
            dos.writeInt(0);
            dos.writeByte(0);
            dos.writeUTF(RedEdit.imp().getServerName());
            dos.close();
            START_MESSAGE = baos.toByteArray();
            ALIVE_MESSAGE = START_MESSAGE.clone();
            ALIVE_MESSAGE[8] = 1;
            DEAD_MESSAGE = START_MESSAGE.clone();
            DEAD_MESSAGE[8] = 2;
        }
        serverStart = new ServerStartEvent();
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                sendMessage(EVERYONE, ALIVE_MESSAGE);
            }
        };
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new Runnable() {
            private final ExecutorService executor = Executors.newSingleThreadExecutor();
            private Future<?> lastExecution;
            @Override
            public void run() {
                if (lastExecution != null && !lastExecution.isDone()) {
                    return;
                }
                lastExecution = executor.submit(task);
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void start() {
        if (!started.getAndSet(true))  {
            sendMessage(EVERYONE, START_MESSAGE);
        }
    }

    @Override
    public Server getServer(int serverId) {
        return this.ALIVE_SERVERS.getIfPresent(serverId);
    }

    @Override
    public Group getGroup(int groupId) {
        return this.ALIVE_GROUPS.getIfPresent(groupId);
    }

    @Override
    public Collection<Server> getServers() {
        return Collections.unmodifiableCollection(ALIVE_SERVERS.asMap().values());
    }

    @Override
    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(ALIVE_GROUPS.asMap().values());
    }

    @Override
    public Channel getServerChannel() {
        return SERVER_CHANNEL;
    }

    @Override
    public Collection<String> getPlayers(String startsWith) {
        Set<String> players = new LinkedHashSet<String>();
        startsWith = startsWith.toLowerCase();
        for (Map.Entry<Integer, Server> entry : this.ALIVE_SERVERS.asMap().entrySet()) {
            Server server = entry.getValue();
            for (String player : server.getPlayers()) {
                if (startsWith.isEmpty() || player.toLowerCase().startsWith(startsWith)) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public Server getServer(String player) {
        for (Map.Entry<Integer, Server> entry : this.ALIVE_SERVERS.asMap().entrySet()) {
            Server server = entry.getValue();
            if (server.hasPlayer(player)) {
                return server;
            }
        }
        return null;
    }

    @Override
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
                        if (server != Settings.IMP.SERVER_ID) {
                            sendMessage(channel, ALIVE_MESSAGE);
                        }
                        setDead(channel);
                    case 1:
                        setAlive(channel, dataStream.readUTF());
                        break;
                    case 2:
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

            Server serverObj = getOrCreateServer(channel);
            if (serverObj == null) {
                Fawe.debug("Server not found: " + channel);
                start();
            }

            switch (type) {
                case RESULT: {
                    packet.result(serverObj, sequence, value);
                    break;
                }
                case ARGUMENT: {
                    packet.argument(serverObj, sequence, value);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isAlive(int groupId, int serverId) {
        if (groupId != 0) {
            Group group = ALIVE_GROUPS.getIfPresent(groupId);
            if (group == null) {
                return false;
            }
        }
        if (serverId != 0) {
            Server server = ALIVE_SERVERS.getIfPresent(serverId);
            if (server != null) {
                return false;
            }
        }
        return true;
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
        start();
        Jedis jedis = POOL.getResource();
        try {
            jedis.publish(id, message);
        } catch (Exception e) {
            POOL.returnBrokenResource(jedis);
        } finally {
            POOL.returnResource(jedis);
        }
    }

    private void setAlive(Channel channel, String name) {
        int groupId = channel.getGroup();
        int serverId = channel.getServer();
        Group existingGroup = this.ALIVE_GROUPS.getIfPresent(groupId);
        Server existingServer = this.ALIVE_SERVERS.getIfPresent(serverId);
        if (existingGroup == null) {
            existingGroup = new Group(groupId, this);
        }
        if (existingServer == null) {
            existingServer = new Server(name, channel, this);
            this.ALIVE_SERVERS.put(serverId, existingServer);
            this.ALIVE_GROUPS.put(groupId, existingGroup);
            serverStart.call(existingServer);
        } else {
            existingServer.setName(name);
            this.ALIVE_SERVERS.put(serverId, existingServer);
            this.ALIVE_GROUPS.put(groupId, existingGroup);
        }
    }

    private Server getOrCreateServer(Channel channel) {
        Server server = getServer(channel.getServer());
        if (server == null) {
            setAlive(channel, null);
        }
        return getServer(channel.getServer());
    }

    private void setDead(Channel channel) {
        int group = channel.getGroup();
        int server = channel.getServer();
        this.ALIVE_SERVERS.invalidate(server);
        this.ALIVE_GROUPS.invalidate(group);
    }
}
