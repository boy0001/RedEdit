package com.boydti.rededit.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.sk89q.worldedit.entity.Player;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.command.teleport.TPAResponse;
import com.boydti.rededit.command.teleport.TeleportRequest;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.remote.Group;
import com.boydti.rededit.remote.Position;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.*;
import com.boydti.rededit.util.plot.PlotLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
                Player player = Fawe.imp().wrap(arg[0]);
                if (player != null) {
                    player.print(arg[1]);
                }
                return null;
            }
        }.setSerializer(new BooleanSerializer(), new StringArraySerializer());

        addTeleportPositionTask = new RemoteCall<Object, Position>() {
            @Override
            public Object run(Server sender, Position pos) {
                int serverId = sender.getId();
                int groupId = sender.getChannel().getGroup();
                System.out.println("position " + pos + " for " + sender);
                if (pos.getPosition() != null) {
                    RedEdit.get().getPlayerListener().addJoinTask(pos.getPlayer(), new Consumer<Player>() {
                        @Override
                        public void accept(Player player) {
                            if (loader != null) loader.disableTeleport(player);
                            final Position back = new Position(pos.getPlayer(), Fawe.imp().getWorldName(player.getWorld()), player.getLocation().toVector(), serverId, groupId);
                            player.setMeta("teleportBack", back);
                            if (loader != null) {
                                loader.disableTeleport(player);
                                if( pos.getWorld() != null) {
                                    loader.load(pos.getWorld());
                                }
                            }
                            Fawe.get().getQueueHandler().sync(new Runnable() {
                                @Override
                                public void run() {
                                    player.findFreePosition(pos.getPosition(player));
                                    player.print(M.TELEPORTING.f("(position) " + "(" + pos + ")"));
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
                int groupId = sender.getChannel().getGroup();
                Player fp = Fawe.imp().wrap(arg[1]);
                if (fp != null) {
                    RedEdit.get().getPlayerListener().addJoinTask(arg[0], new Consumer<Player>() {
                        @Override
                        public void accept(Player player) {
                            if (loader != null) loader.disableTeleport(player);
                            final Position back = new Position(player.getName(), Fawe.imp().getWorldName(player.getWorld()), player.getLocation().toVector(), serverId, groupId);
                            player.setMeta("teleportBack", back);
                            Location to = player.getLocation();
                            player.findFreePosition(to);
                            player.print(M.TELEPORTING.f("(player) " + "(" + fp.getName() + ")"));
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
                Player player = Fawe.imp().wrap(request.receiver);
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

    public boolean addRequest(Player fp, TeleportRequest request) {
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

    public TeleportRequest getRequest(Player fp, String player) {
        LoadingCache<String, TeleportRequest> requests = fp.getMeta("teleportRequests");
        if (player == null || player.isEmpty()) {
            for (Map.Entry<String, TeleportRequest> entry : requests.asMap().entrySet()) {
                return entry.getValue();
            }
            return null;
        }
        return requests.getIfPresent(player.toLowerCase());
    }

    public TeleportRequest removeRequest(Player fp, String player) {
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

    public boolean back(Player fp) {
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
            Location pos = fp.getLocation();
            fp.setMeta("teleportBack", new Position(fp.getName(), Fawe.imp().getWorldName((World) pos.getExtent()), pos, Settings.IMP.SERVER_ID, Settings.IMP.SERVER_GROUP));
            String world = previous.getWorld();
            if (world != null && loader != null) {
                Server server = loader.getLoadedServer(world);
                if (server != null) {
                    Vector3 v = previous.getPosition();
                    teleport(fp, server.getId(), server.getChannel().getGroup(), world, v);
                    return true;
                }
            }
            if (loader != null) loader.disableTeleport(fp);
            if (loader != null && previous.getWorld() != null) {
                loader.load(previous.getWorld());
            }
            Fawe.get().getQueueHandler().sync(new Runnable() {
                @Override
                public void run() {
                    fp.findFreePosition(previous.getPosition(fp));
                }
            });
            return true;
        }
        return false;
    }

    public boolean teleport(Player from, Position pos) {
        return teleport(from, pos.getServer(), pos.getGroup(), pos.getWorld(), pos.getPosition());
    }

    public boolean teleport(Player from, Integer serverId, Integer groupId, String world, Vector3 pos) {
        if (serverId == null || serverId == Integer.MIN_VALUE) {
            serverId = Settings.IMP.SERVER_ID;
        }

        Network network = RedEdit.get().getNetwork();
        Server server = serverId == null || serverId == Integer.MIN_VALUE ? network.getLocalServer() : network.getServer(serverId);
        groupId = groupId != null && groupId != Integer.MIN_VALUE ? groupId : (server != null ? server.getChannel().getGroup() : null);
        if (loader != null) {
            Server newServer;
            if (groupId != null) {
                newServer = loader.getLoadedServer(world, groupId);
            } else {
                newServer = loader.getLoadedServer(world);
            }
            if (newServer != null) {
                server = newServer;
                serverId = newServer.getId();
            } else if (server == null && groupId != null) {
                Group group = RedEdit.get().getNetwork().getGroup(groupId);
                if (group != null) {
                    Collection<Server> servers = group.getServers();
                    if (servers != null && !servers.isEmpty()) {
                        server = servers.iterator().next();
                        serverId = server.getId();
                    }
                }
            }
        }
        if (server == null) {
            return false;
        }
        if (serverId == null) {
            System.out.println("Invalid server id: " + serverId + " loader: " + loader);
            serverId = server.getId();
        }
        Position remotePos = new Position(from.getName(), world, pos, serverId, groupId);
        addTeleportPositionTask.call(0, serverId, remotePos, null);
        RedEdit.imp().teleport(from, server.getName());
        return true;
    }

    public void teleport(String from, Player playerTo) {
        Player fpFrom = Fawe.imp().wrap(from);
//        if (fpFrom != null) {
//            fpFrom.getPlayer().setPosition(playerTo.getPlayer().getPosition());
//        } else
        {
            final Position back;
            if (fpFrom != null) {
                back = new Position(from, Fawe.imp().getWorldName(fpFrom.getWorld()), fpFrom.getLocation().toVector(), Settings.IMP.SERVER_ID, null);
            } else {
                Server server = RedEdit.get().getNetwork().getServer(from);
                back = new Position(from, null, null, server != null ? server.getId() : Settings.IMP.SERVER_ID, null);
            }
            RedEdit.imp().teleportHere(playerTo, from);
            RedEdit.get().getPlayerListener().addJoinTask(from, new Consumer<Player>() {
                @Override
                public void accept(Player player) {
                    if (loader != null) loader.disableTeleport(player);
                    Fawe.get().getQueueHandler().sync(new Runnable() {
                        @Override
                        public void run() {
                            player.setMeta("teleportBack", back);
                            if (loader != null) loader.disableTeleport(player);
                            player.findFreePosition(playerTo.getLocation());
                        }
                    });
                }
            });
        }
    }

    public void teleport(Player playerFrom, String playerTo) {
        Player fpTo = Fawe.imp().wrap(playerTo);
//        if (fpTo != null) {
//            Player wePlrTo = fpTo.getPlayer();
//            wePlrTo.setPosition(playerFrom.getPosition());
//        } else
        {
            String playerFromName = playerFrom.getName();
            addTeleportPlayerTask.call(0, 0, new String[]{ playerFromName, playerTo }, new RunnableVal2<Server, Object>() {
                @Override
                public void run(Server server, Object o) {
                    Fawe.get().getQueueHandler().sync(new Runnable() {
                        @Override
                        public void run() {
                            RedEdit.imp().teleport(playerFrom, server.getName());
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
