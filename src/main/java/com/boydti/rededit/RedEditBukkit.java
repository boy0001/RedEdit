package com.boydti.rededit;

import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.plugin.java.JavaPlugin;

public class RedEditBukkit extends JavaPlugin implements IRedEditPlugin, Listener {

    private PlayerJoinEvent playerJoin = new PlayerJoinEvent();
    private PlayerQuitEvent playerQuit = new PlayerQuitEvent();

    @Override
    public void onEnable() {
        RedEdit.init(this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, this);
        final int timeout = 2;
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                int now = (int) (System.currentTimeMillis() / 1000);
                for (Map.Entry<UUID, int[]> entry : views.entrySet()) {
                    int[] value = entry.getValue();
                    if (now - value[1] > timeout) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        setViewDistance(player, Math.max(4, value[0] + 1));
                    }
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
        return Bukkit.getServerName();
    }

    @Override
    public void teleport(FawePlayer fp, String server) {
        if (server.equals(getServerName())) {
            return;
        }
        Player player = ((BukkitPlayer) fp).parent;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void teleportHere(FawePlayer fp, String otherPlayer) {
        Player other = Bukkit.getPlayer(otherPlayer);
        if (other != null) {
            return;
        }
        Player player = ((BukkitPlayer) fp).parent;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(otherPlayer);
        out.writeUTF(getServerName());
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        List<String> tabs = event.getCompletions();
        String[] buffer = event.getBuffer().split(" ");
        String msg = buffer[buffer.length - 1].toLowerCase();
        if (msg.length() < 16 || msg.length() > 0) {
            Collection<String> toAdd = RedEdit.get().getNetwork().getPlayers(msg);
            if (toAdd.isEmpty()) {
                ArrayList<String> both = new ArrayList<>(tabs);
                both.addAll(tabs);
                event.setCompletions(both);
            }
        }
    }

    private Map<UUID, int[]> views = new ConcurrentHashMap<>();

    public void setViewDistance(Player player, int value) {
        UUID uuid = player.getUniqueId();
        if (value == 10) {
            views.remove(uuid);
        } else {
            int[] val = views.get(uuid);
            if (val == null) {
                val = new int[] {value, (int) (System.currentTimeMillis() / 1000)};
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

    public int getViewDistance(Player player) {
        int[] value = views.get(player.getUniqueId());
        return value == null ? 10 : value[0];
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        setViewDistance(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() >> 6 != to.getBlockX() >> 6 || from.getBlockZ() >> 6 != to.getBlockZ() >> 6) {
            Player player = event.getPlayer();
            int currentView = getViewDistance(player);
            setViewDistance(player, Math.max(currentView - 1, 1));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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