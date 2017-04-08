package com.boydti.rededit;

import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.events.PlayerJoinEvent;
import com.boydti.rededit.events.PlayerQuitEvent;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RedEditBukkit extends JavaPlugin implements IRedEditPlugin, Listener {

    private RedEdit rededit;
    private PlayerJoinEvent playerJoin = new PlayerJoinEvent();
    private PlayerQuitEvent playerQuit = new PlayerQuitEvent();

    @Override
    public void onEnable() {
        try {
            this.rededit = new RedEdit(this);
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        this.rededit.close();
    }

    @Override
    public void teleport(FawePlayer fp, int server) {
        if (server == Settings.IMP.SERVER_ID) {
            return;
        }
        Player player = ((BukkitPlayer) fp).parent;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(Integer.toString(server));
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
        out.writeUTF(Integer.toString(Settings.IMP.SERVER_ID));
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        FawePlayer fp = FawePlayer.wrap(event.getPlayer());
        playerJoin.call(fp);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(org.bukkit.event.player.PlayerQuitEvent event) {
        FawePlayer fp = FawePlayer.wrap(event.getPlayer());
        playerQuit.call(fp);
    }
}