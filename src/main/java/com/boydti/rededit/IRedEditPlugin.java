package com.boydti.rededit;

import com.boydti.fawe.object.FawePlayer;

public interface IRedEditPlugin {
    public void teleport(FawePlayer fp, int server);

    public void teleportHere(FawePlayer desination, String otherPlayer);

    public void registerEvents();
}
