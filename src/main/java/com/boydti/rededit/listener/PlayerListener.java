package com.boydti.rededit.listener;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
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

    public PlayerListener() {
        WorldEdit.getInstance().getEventBus().register(this);
        this.joinTasks = MapUtil.getExpiringMap(10, TimeUnit.SECONDS);
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
        // TODO?
    }
}
