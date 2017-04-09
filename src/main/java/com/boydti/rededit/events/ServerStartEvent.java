package com.boydti.rededit.events;

import com.boydti.rededit.remote.Server;
import com.sk89q.worldedit.WorldEdit;

public class ServerStartEvent {
    private Server server;

    public ServerStartEvent() {}

    public void call(Server server) {
        this.server = server;
        WorldEdit.getInstance().getEventBus().post(this);
    }

    public Server getServer() {
        return server;
    }
}
