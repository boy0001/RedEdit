package com.boydti.rededit.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.command.teleport.TPAResponse;
import com.boydti.rededit.command.teleport.TeleportRequest;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.remote.Position;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.BooleanSerializer;
import com.boydti.rededit.serializer.EnumSerializer;
import com.boydti.rededit.serializer.PositionSerializer;
import com.boydti.rededit.serializer.StringArraySerializer;
import com.boydti.rededit.serializer.VoidSerializer;
import com.boydti.rededit.util.plot.PlotLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.Player;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TeleportUtil {

    private final PlotLoader loader;
    private RemoteCall<Boolean, String[]> message;
    private RemoteCall<TPAResponse, TeleportRequest> tpa;
    private RemoteCall<Object, String[]> addTeleportPlayerTask;
    private RemoteCall<Object, Position> addTeleportPositionTask;

    public TeleportUtil(PlotLoader loader) {
        this.loader = loader;
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

        addTeleportPositionTask = new RemoteCall<Object, Position>() {
            @Override
            public Object run(Server sender, Position pos) {
                int serverId = sender.getId();
                if (pos.getPosition() != null) {
                    RedEdit.get().getPlayerListener().addJoinTask(pos.getPlayer(), new RunnableVal<FawePlayer>() {
                        @Override
                        public void run(FawePlayer fawePlayer) {
                            if (loader != null) loader.disableTeleport(fawePlayer);
                            Player player = fawePlayer.getPlayer();
                            final Position back = new Position(pos.getPlayer(), Fawe.imp().getWorldName(player.getWorld()), player.getPosition(), serverId);
                            fawePlayer.setMeta("teleportBack", back);
                            TaskManager.IMP.sync(new RunnableVal<Object>() {
                                @Override
                                public void run(Object o) {
                                    if (loader != null) {
                                        loader.disableTeleport(fawePlayer);
                                        if( pos.getWorld() != null) {
                                            loader.load(pos.getWorld());
                                        }
                                    }
                                    player.findFreePosition(pos.getPosition(fawePlayer));
                                }
                            });
                        }
                    });
                }
                return null;
            }
        }.setSerializer(new VoidSerializer(), new PositionSerializer());

        addTeleportPlayerTask = new RemoteCall<Object, String[]>() {
            @Override
            public Object run(Server sender, String[] arg) {
                int serverId = sender.getId();
                FawePlayer<Object> fp = FawePlayer.wrap(arg[1]);
                if (fp != null) {
                    RedEdit.get().getPlayerListener().addJoinTask(arg[0], new RunnableVal<FawePlayer>() {
                        @Override
                        public void run(FawePlayer fawePlayer) {
                            if (loader != null) loader.disableTeleport(fawePlayer);
                            Player player = fawePlayer.getPlayer();
                            final Position back = new Position(player.getName(), Fawe.imp().getWorldName(player.getWorld()), player.getPosition(), serverId);
                            fawePlayer.setMeta("teleportBack", back);
                            player.findFreePosition(fp.getPlayer().getPosition());
                        }
                    });
                    return true;
                }
                return null;
            }
        }.setSerializer(new VoidSerializer(), new StringArraySerializer());

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
            Server server = RedEdit.get().getNetwork().getServer(previous.getServer());
            if (server != null) {
                RedEdit.imp().teleport(fp, server.getName());
            }
            return true;
        } else if (previous.getPosition() != null) {
            WorldVector pos = fp.getPlayer().getPosition();
            fp.setMeta("teleportBack", new Position(fp.getName(), Fawe.imp().getWorldName(pos.getWorld()), pos, Settings.IMP.SERVER_ID));
            String world = previous.getWorld();
            if (world != null && loader != null) {
                Server server = loader.getLoadedServer(world);
                if (server != null) {
                    Vector v = previous.getPosition();
                    teleport(fp, server.getId(), world, v);
                    return true;
                }
            }
            if (loader != null) loader.disableTeleport(fp);
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object o) {
                    if (loader != null && previous.getWorld() != null) {
                        loader.load(previous.getWorld());
                    }
                    fp.getPlayer().findFreePosition(previous.getPosition(fp));
                }
            });
            return true;
        }
        return false;
    }

    public boolean teleport(FawePlayer from, Position pos) {
        return teleport(from, pos.getServer(), pos.getWorld(), pos.getPosition());
    }

    public boolean teleport(FawePlayer from, Integer serverId, String world, Vector pos) {
        if (serverId == null || serverId == 0) {
            serverId = Settings.IMP.SERVER_ID;
        }

        Network network = RedEdit.get().getNetwork();
        Server server = serverId == null || serverId == 0 ? network.getLocalServer() : network.getServer(serverId);
        if (loader != null) {
            Server newServer;
            if (server != null) {
                newServer = loader.getLoadedServer(world, server.getChannel().getGroup());
            } else {
                newServer = loader.getLoadedServer(world);
            }
            if (newServer != null) {
                server = newServer;
                serverId = newServer.getId();
            }
        }
        if (server == null) {
            return false;
        }
        Position remotePos = new Position(from.getName(), world, pos, serverId);
        addTeleportPositionTask.call(0, serverId, remotePos, null);
        RedEdit.imp().teleport(from, server.getName());
        return true;
    }

    public void teleport(String from, FawePlayer playerTo) {
        FawePlayer fpFrom = FawePlayer.wrap(from);
//        if (fpFrom != null) {
//            fpFrom.getPlayer().setPosition(playerTo.getPlayer().getPosition());
//        } else
        {
            final Position back;
            if (fpFrom != null) {
                back = new Position(from, Fawe.imp().getWorldName(fpFrom.getWorld()), fpFrom.getPlayer().getPosition(), Settings.IMP.SERVER_ID);
            } else {
                Server server = RedEdit.get().getNetwork().getServer(from);
                back = new Position(from, null, null, server != null ? server.getId() : Settings.IMP.SERVER_ID);
            }
            RedEdit.imp().teleportHere(playerTo, from);
            RedEdit.get().getPlayerListener().addJoinTask(from, new RunnableVal<FawePlayer>() {
                @Override
                public void run(FawePlayer fawePlayer) {
                    if (loader != null) loader.disableTeleport(fawePlayer);
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object o) {
                            fawePlayer.setMeta("teleportBack", back);
                            if (loader != null) loader.disableTeleport(fawePlayer);
                            fawePlayer.getPlayer().findFreePosition(playerTo.getPlayer().getPosition());
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
                            RedEdit.imp().teleport(from, server.getName());
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
