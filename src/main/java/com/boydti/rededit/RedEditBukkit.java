package com.boydti.rededit;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
import com.boydti.rededit.util.plot.PlotLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RedEditBukkit extends JavaPlugin implements IRedEditPlugin, Listener {

    private PlayerJoinEvent playerJoin = new PlayerJoinEvent();
    private PlayerQuitEvent playerQuit = new PlayerQuitEvent();
    private IRedEditPlugin imp;
    private static int OFFSET = 6;
    private static int MAX_DISTANCE = 15;

    @Override
    public void onEnable() {
        try {
            imp = new RedEditLily();
        } catch (Throwable e) {
            imp = new RedEditBungee(this);
        }
        RedEdit.init(this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        final int timeout = 1;
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (Settings.IMP.DYNAMIC_RENDERING) {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    Iterator<Map.Entry<UUID, int[]>> iter = views.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<UUID, int[]> entry = iter.next();
                        int[] value = entry.getValue();
                        if (now - value[1] > timeout) {
                            Player player = Bukkit.getPlayer(entry.getKey());
                            if (player != null) {
                                setViewDistance(player, Math.max(4, value[0] + 1));
                            } else {
                                iter.remove();
                            }
                        }
                    }
                } else if (!views.isEmpty()) {
                    views.clear();
                }
            }
        }, 20);
    }

    @Override
    public void onDisable() {
        RedEdit.get().close();
    }

    @Override
    public String getServerName() {
        return imp.getServerName();
    }

    @Override
    public void teleport(FawePlayer fp, String server) {
        imp.teleport(fp, server);
    }

    @Override
    public void teleportHere(FawePlayer fp, String otherPlayer) {
        imp.teleportHere(fp, otherPlayer);
    }

    @Override
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        PlotLoader loader = RedEdit.get().getPlotLoader();
        if (loader != null) {
            loader.claim(event.getWorld().getName());
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        List<String> tabs = event.getCompletions();
        String[] buffer = event.getBuffer().split(" ");
        String msg = buffer[buffer.length - 1].toLowerCase();
        if (msg.length() < 16 && msg.length() > 0) {
            Collection<String> toAdd = RedEdit.get().getNetwork().getPlayers(msg);
            if (!toAdd.isEmpty()) {
                ArrayList<String> both = new ArrayList<>(tabs);
                both.addAll(toAdd);
                event.setCompletions(both);
            }
        }
    }

    private Map<UUID, int[]> views = new ConcurrentHashMap<>();

    public void setViewDistance(Player player, int value) {
        if (Settings.IMP.DYNAMIC_RENDERING) {
            UUID uuid = player.getUniqueId();
            if (value == MAX_DISTANCE) {
                views.remove(uuid);
            } else {
                int[] val = views.get(uuid);
                if (val == null) {
                    val = new int[]{value, (int) (System.currentTimeMillis() / 1000)};
                    views.put(player.getUniqueId(), val);
                } else {
                    if (value <= val[0]) {
                        val[1] = (int) (System.currentTimeMillis() / 1000);
                    }
                    if (val[0] == value) {
                        return;
                    } else {
                        val[0] = value;
                    }
                }
            }
            player.setViewDistance(value);
        }
    }

    public int getViewDistance(Player player) {
        int[] value = views.get(player.getUniqueId());
        return value == null ? MAX_DISTANCE : value[0];
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        setViewDistance(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (Settings.IMP.DYNAMIC_RENDERING) {
            if (from.getBlockX() >> 6 != to.getBlockX() >> OFFSET || from.getBlockZ() >> OFFSET != to.getBlockZ() >> OFFSET) {
                Player player = event.getPlayer();
                int currentView = getViewDistance(player);
                setViewDistance(player, Math.max(currentView - 1, 1));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setViewDistance(player, 1);
        FawePlayer fp = FawePlayer.wrap(player);
        playerJoin.call(fp);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        views.remove(player.getUniqueId());
        FawePlayer fp = FawePlayer.wrap(player);
        playerQuit.call(fp);
    }
}