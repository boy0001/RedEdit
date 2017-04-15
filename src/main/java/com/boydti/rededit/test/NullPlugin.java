package com.boydti.rededit.test;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.rededit.IRedEditPlugin;

public class NullPlugin implements IRedEditPlugin {
    @Override
    public void teleport(FawePlayer fp, String server) {

    }

    @Override
    public void teleportHere(FawePlayer desination, String otherPlayer) {

    }

    @Override
    public void registerEvents() {

    }

    @Override
    public String getServerName() {
        return "TEST";
    }
}
