package com.boydti.rededit;

import com.sk89q.worldedit.entity.Player;

public interface IRedEditPlugin {
    void teleport(Player fp, String server);

    void teleportHere(Player desination, String otherPlayer);

    void registerEvents();

    String getServerName();
}
