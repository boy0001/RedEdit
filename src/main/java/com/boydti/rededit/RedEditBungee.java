package com.boydti.rededit;

import com.boydti.fawe.bukkit.BukkitPlayer;
import com.sk89q.worldedit.entity.Player;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RedEditBungee implements IRedEditPlugin {

    private final Plugin plugin;

    public RedEditBungee(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getServerName() {
        return Bukkit.getServerName();
    }

    @Override
    public void teleport(Player fp, String server) {
        if (server.equals(getServerName())) {
            return;
        }
        Player player = ((BukkitPlayer) fp).parent;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void teleportHere(Player fp, String otherPlayer) {
        Player other = Bukkit.getPlayer(otherPlayer);
        if (other != null) {
            return;
        }
        Player player = ((BukkitPlayer) fp).parent;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(otherPlayer);
        out.writeUTF(getServerName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void registerEvents() {

    }
}
