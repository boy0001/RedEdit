package com.boydti.rededit;

import com.sk89q.worldedit.entity.Player;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.request.impl.RedirectRequest;
import lilypad.client.connect.api.result.FutureResultListener;
import lilypad.client.connect.api.result.StatusCode;
import lilypad.client.connect.api.result.impl.RedirectResult;
import org.bukkit.Bukkit;

public class RedEditLily implements IRedEditPlugin {
    private final Connect connect;
    private final String server;

    public RedEditLily() {
        this.connect = Bukkit.getServer().getServicesManager().getRegistration(Connect.class).getProvider();
        this.connect.registerEvents(this);
        this.server = this.connect.getSettings().getUsername();
    }

    @Override
    public void teleport(Player fp, String server) {
        if (server.equals(this.getServerName())) {
            return;
        }
        try {
            this.connect.request(new RedirectRequest(server, fp.getName())).registerListener(new FutureResultListener<RedirectResult>() {
                @Override
                public void onResult(final RedirectResult redirectResult) {
                    if (redirectResult.getStatusCode() == StatusCode.SUCCESS) {
                        return;
                    }
                    fp.print("Could not connect");
                }
            });
        } catch (final Exception exception) {
            fp.print("Could not connect");
        }
    }

    @Override
    public void teleportHere(Player desination, String otherPlayer) {
        try {
            this.connect.request(new RedirectRequest(server, otherPlayer)).registerListener(new FutureResultListener<RedirectResult>() {
                @Override
                public void onResult(final RedirectResult redirectResult) {
                    if (redirectResult.getStatusCode() == StatusCode.SUCCESS) {
                        return;
                    }
                }
            });
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void registerEvents() {

    }

    @Override
    public String getServerName() {
        return server;
    }
}
