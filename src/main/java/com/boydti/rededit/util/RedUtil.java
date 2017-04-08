package com.boydti.rededit.util;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.command.teleport.TPAResponse;
import com.boydti.rededit.command.teleport.TeleportRequest;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.Position;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.BooleanSerializer;
import com.boydti.rededit.serializer.EnumSerializer;
import com.boydti.rededit.serializer.StringArraySerializer;
import com.boydti.rededit.serializer.StringSerializer;
import com.boydti.rededit.serializer.VoidSerializer;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.entity.Player;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedUtil {

    private RemoteCall<Boolean, String[]> message;
    private RemoteCall<TPAResponse, TeleportRequest> tpa;
    private RemoteCall<Object, String[]> addTeleportPlayerTask;

    public RedUtil() {
        this.message = new RemoteCall<Boolean, String[]>() {
            @Override
            public Boolean run(Server sender, String[] arg) {
                FawePlayer<Object> player = FawePlayer.wrap(arg[0]);
                if (player != null) {
                    player.sendMessage(arg[1]);
                }
                return null;
            }
        }.setSerializer(new BooleanSerializer(), new StringArraySerializer());

        addTeleportPlayerTask = new RemoteCall<Object, String[]>() {
            @Override
            public Object run(Server sender, String[] arg) {
                int serverId = sender.getId();
                FawePlayer<Object> fp = FawePlayer.wrap(arg[0]);
                if (fp != null) {
                    RedEdit.get().getPlayerListener().addJoinTask(arg[1], new RunnableVal<FawePlayer>() {
                        @Override
                        public void run(FawePlayer fawePlayer) {
                            Player player = fawePlayer.getPlayer();
                            final Position back = new Position(player.getPosition(), serverId);
                            fawePlayer.setMeta("teleportBack", back);
                            player.setPosition(fp.getPlayer().getPosition());
                        }
                    });
                    return true;
                }
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringSerializer());

        this.tpa = new RemoteCall<TPAResponse, TeleportRequest>() {
            @Override
            public TPAResponse run(Server sender, TeleportRequest request) {
                FawePlayer player = FawePlayer.wrap(request.receiver);
                if (player != null) {
                    if (request.override || player.hasPermission("rededit.tp.disabled")) {
                        if (!addRequest(player, request)) {
                            return TPAResponse.DUPLICATE;
                        }
                        if (request.to) {
                            M.TPA_REQUEST.send(player, request.sender);
                        } else {
                            M.TPAHERE_REQUEST.send(player, request.sender);
                        }
                        return TPAResponse.ALLOW;
                    } else {
                        return TPAResponse.DENY;
                    }
                }
                return null;
            }
        }.setSerializer(new EnumSerializer<>(TPAResponse.values()), TeleportRequest.getSerializer());
    }

    public void tpa(TeleportRequest request, RunnableVal2<Server, TPAResponse> run) {
        tpa.call(0, 0, request, run);
    }

    public boolean addRequest(FawePlayer fp, TeleportRequest request) {
        synchronized (fp) {
            LoadingCache<String, TeleportRequest> requests = fp.getMeta("teleportRequests");
            if (requests == null) {
                requests = MapUtil.getExpiringMap(60, TimeUnit.SECONDS);
                fp.setMeta("teleportRequests", requests);
            }
            String name = request.sender.toLowerCase();
            if (requests.getIfPresent(name) != null) {
                return false;
            }
            requests.put(name, request);
            return true;
        }
    }

    public TeleportRequest getRequest(FawePlayer fp, String player) {
        LoadingCache<String, TeleportRequest> requests = fp.getMeta("teleportRequests");
        if (player == null || player.isEmpty()) {
            for (Map.Entry<String, TeleportRequest> entry : requests.asMap().entrySet()) {
                return entry.getValue();
            }
            return null;
        }
        return requests.getIfPresent(player.toLowerCase());
    }

    public TeleportRequest removeRequest(FawePlayer fp, String player) {
        LoadingCache<String, TeleportRequest> requests = fp.getMeta("teleportRequests");
        if (player == null || player.isEmpty()) {
            TeleportRequest result = null;
            for (Map.Entry<String, TeleportRequest> entry : requests.asMap().entrySet()) {
                result = entry.getValue();
            }
            if (result != null) {
                requests.invalidate(result.sender.toLowerCase());
            }
            return result;
        }
        return requests.getIfPresent(player.toLowerCase());
    }

    public boolean back(FawePlayer fp) {
        Position previous = fp.getMeta("teleportBack");
        if (previous == null) {
            return false;
        }
        if (previous.getServer() != null && previous.getServer() != Settings.IMP.SERVER_ID){
            RedEdit.imp().teleport(fp, previous.getServer());
            return true;
        } else if (previous.getPosition() != null) {
            fp.getPlayer().setPosition(previous.getPosition());
            return true;
        }
        return false;
    }

    public void teleport(String from, FawePlayer playerTo) {
        FawePlayer fpFrom = FawePlayer.wrap(from);
//        if (fpFrom != null) {
//            fpFrom.getPlayer().setPosition(playerTo.getPlayer().getPosition());
//        } else
        {
            final Position back = new Position(fpFrom != null ? fpFrom.getPlayer().getPosition() : null, Settings.IMP.SERVER_ID);
            RedEdit.imp().teleportHere(playerTo, from);
            RedEdit.get().getPlayerListener().addJoinTask(from, new RunnableVal<FawePlayer>() {
                @Override
                public void run(FawePlayer fawePlayer) {
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object o) {
                            fawePlayer.setMeta("teleportBack", back);
                            fawePlayer.getPlayer().setPosition(playerTo.getPlayer().getPosition());
                        }
                    });
                }
            });
        }
    }

    public void teleport(FawePlayer from, String playerTo) {
        Player playerFrom = from.getPlayer();
        FawePlayer fpTo = FawePlayer.wrap(playerTo);
//        if (fpTo != null) {
//            Player wePlrTo = fpTo.getPlayer();
//            wePlrTo.setPosition(playerFrom.getPosition());
//        } else
        {
            String playerFromName = from.getName();
            addTeleportPlayerTask.call(0, 0, new String[]{ playerFromName, playerTo }, new RunnableVal2<Server, Object>() {
                @Override
                public void run(Server server, Object o) {
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object o) {
                            RedEdit.imp().teleport(from, server.getId());
                        }
                    });
                }
            });
        }
    }

    public void sendMessage(String player, String message) {
        this.message.call(0, 0, new String[]{player, message}, null);
    }
}
