package com.boydti.rededit.test;

import com.sk89q.worldedit.entity.Player;
import com.boydti.rededit.IRedEditPlugin;

public class NullPlugin implements IRedEditPlugin {
    @Override
    public void teleport(Player fp, String server) {

    }

    @Override
    public void teleportHere(Player desination, String otherPlayer) {

    }

    @Override
    public void registerEvents() {

    }

    @Override
    public String getServerName() {
        return "PLEASE INITIALIZE THE PLUGIN";
    }
}
