package com.boydti.rededit.listener;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.StringSerializer;
import com.boydti.rededit.serializer.VoidSerializer;
import com.boydti.rededit.util.MapUtil;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static com.google.common.base.Preconditions.checkNotNull;

public class PlayerListener {

    private final LoadingCache<String, List<RunnableVal<FawePlayer>>> joinTasks;
    private final RemoteCall<Object, String> joinPacket;
    private final RemoteCall<Object, String> quitPacket;
    private final ServerController sub;

    public PlayerListener(ServerController sub) {
        this.sub = sub;
        WorldEdit.getInstance().getEventBus().register(this);
        this.joinTasks = MapUtil.getExpiringMap(10, TimeUnit.SECONDS);
        this.joinPacket = new RemoteCall<Object, String>() {
            @Override
            public Object run(Server sender, String player) {
                sender.addPlayer(player);
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringSerializer());
        this.quitPacket = new RemoteCall<Object, String>() {
            @Override
            public Object run(Server sender, String player) {
                sender.removePlayer(player);
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringSerializer());
    }

    public void addJoinTask(String playerName, RunnableVal<FawePlayer> task) {
        checkNotNull(task);
        FawePlayer player;
        synchronized (joinTasks) {
            player = FawePlayer.wrap(playerName);
            if (player == null) {
                List<RunnableVal<FawePlayer>> tasks = joinTasks.getIfPresent(playerName);
                if (joinTasks == null) {
                    tasks = new ArrayList<>();
                }
                tasks.add(task);
                joinTasks.put(playerName, tasks);
                return;
            }
        }
        task.run(player);
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        FawePlayer player = event.getPlayer();
        String name = player.getName();
        joinPacket.call(0, 0, name);
        List<RunnableVal<FawePlayer>> tasks;
        synchronized (joinTasks) {
            tasks = joinTasks.getIfPresent(name);
            if (tasks == null) {
                return;
            }
            joinTasks.invalidate(name);
        }
        for (RunnableVal<FawePlayer> task : tasks) {
            try {
                task.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event) {
        FawePlayer fp = event.getPlayer();
        String name = fp.getName();
        quitPacket.call(0, 0, name);
        // TODO?
    }
}
