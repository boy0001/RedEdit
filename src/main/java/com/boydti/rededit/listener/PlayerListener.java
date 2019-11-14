package com.boydti.rededit.listener;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.entity.Player;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
import com.boydti.rededit.events.ServerStartEvent;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.StringArraySerializer;
import com.boydti.rededit.serializer.VoidSerializer;
import com.boydti.rededit.util.MapUtil;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


import static com.google.common.base.Preconditions.checkNotNull;

public class PlayerListener {

    private final LoadingCache<String, List<Consumer<Player>>> joinTasks;
    private final RemoteCall<Object, String[]> joinPacket;
    private final RemoteCall<Object, String[]> quitPacket;
    private final Network sub;

    public PlayerListener(Network sub) {
        this.sub = sub;
        WorldEdit.getInstance().getEventBus().register(this);
        this.joinTasks = MapUtil.getExpiringMap(10, TimeUnit.SECONDS);
        this.joinPacket = new RemoteCall<Object, String[]>() {
            @Override
            public Object run(Server sender, String[] players) {
                for (String player : players) sender.addPlayer(player);
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringArraySerializer());
        this.quitPacket = new RemoteCall<Object, String[]>() {
            @Override
            public Object run(Server sender, String[] players) {
                for (String player : players) sender.removePlayer(player);
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringArraySerializer());
    }

    public void addJoinTask(String playerName, Consumer<Player> task) {
        checkNotNull(task);
        Player player;
        synchronized (joinTasks) {
            player = Fawe.imp().wrap(playerName);
            if (player == null) {
                List<Consumer<Player>> tasks = joinTasks.getIfPresent(playerName);
                if (tasks == null) {
                    tasks = new ArrayList<>();
                }
                tasks.add(task);
                joinTasks.put(playerName, tasks);
                return;
            }
        }
        task.accept(player);
    }

    @Subscribe
    public void onServerStart(ServerStartEvent event) {
        Server server = event.getServer();
        Server thisServer = RedEdit.get().getNetwork().getServer(Settings.IMP.SERVER_ID);
        if (thisServer != null) {
            Collection<String> players = thisServer.getPlayers();
            joinPacket.call(0, server.getId(), players.toArray(new String[players.size()]));
        } else {
            RedEdit.debug("Cannot find server: " + Settings.IMP.SERVER_ID);
        }
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        joinPacket.call(0, 0, new String[] { name });
        List<Consumer<Player>> tasks;
        synchronized (joinTasks) {
            tasks = joinTasks.getIfPresent(name);
            if (tasks == null) {
                return;
            }
            joinTasks.invalidate(name);
        }
        for (Consumer<Player> task : tasks) {
            try {
                task.accept(player);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player fp = event.getPlayer();
        String name = fp.getName();
        quitPacket.call(0, 0, new String[] { name });
        // TODO?
    }
}
