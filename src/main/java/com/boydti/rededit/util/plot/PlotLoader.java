package com.boydti.rededit.util.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.ResultCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.util.MapUtil;
import com.google.common.cache.LoadingCache;
import com.intellectualcrafters.configuration.ConfigurationSection;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;
import com.intellectualcrafters.plot.util.WorldUtil;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlotLoader {

    private final RemoteCall<Object, PlotData> syncPlot;
    private final RemoteCall<Object, PlotData> deletePlot;
    private final RemoteCall<Boolean, String> isLoaded;
    private final RemoteCall<Long, String> getClaimed;
    private final RemoteCall<Object, String[]> teleport;

    private final LoadingCache<String, Long> claims = MapUtil.getExpiringMap(1000L, TimeUnit.MILLISECONDS);

    public PlotLoader() {
        PS.get();
        this.syncPlot = new RemoteCall<Object, PlotData>() {
            @Override
            public Object run(Server sender, PlotData plot) {
                if (sender.getId() != Settings.IMP.SERVER_ID) {
                    plot.load();
                }
                return null;
            }
        };
        this.deletePlot = new RemoteCall<Object, PlotData>() {
            @Override
            public Object run(Server sender, PlotData arg) {
                if (sender.getId() != Settings.IMP.SERVER_ID) arg.remove();
                return null;
            }
        };
        this.isLoaded = new RemoteCall<Boolean, String>() {
            @Override
            public Boolean run(Server sender, String worldName) {
                if (WorldUtil.IMP.isWorld(worldName)) {
                    return true;
                }
                return false;
            }
        };
        this.getClaimed = new RemoteCall<Long, String>() {
            @Override
            public Long run(Server sender, String worldName) {
                if (WorldUtil.IMP.isWorld(worldName)) {
                    return 0l;
                }
                Long claimedTime = claims.getIfPresent(worldName);
                if (claimedTime != null) {
                    return claimedTime;
                }
                return null;
            }
        };
        this.teleport = new RemoteCall<Object, String[]>() {
            @Override
            public Object run(Server sender, String[] args) {
                String player = args[0];
                String world = args[1];
                String areaId = args[2];
                String idStr = args[3];
                PlotArea area = PS.get().getPlotArea(world, areaId);
                if (area != null) {
                    PlotId id = PlotId.fromString(idStr);
                    Plot plot = area.getOwnedPlot(id);
                    if (plot != null) {
                        RedEdit.get().getPlayerListener().addJoinTask(player, new RunnableVal<FawePlayer>() {
                            @Override
                            public void run(FawePlayer fawePlayer) {
                                disableTeleport(fawePlayer);
                                PlotPlayer pp = PlotPlayer.wrap(fawePlayer.getName());
                                plot.teleportPlayer(pp);
                            }
                        });
                        return null;
                    }
                }
                return null;
            }
        };
        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            SinglePlotAreaManager singleManager = (SinglePlotAreaManager) manager;
            SinglePlotArea area = singleManager.getArea();
            NetworkPlotArea newArea = new NetworkPlotArea(this);
            singleManager.setArea(newArea);
            ConfigurationSection section = PS.get().worlds.getConfigurationSection("worlds.*");
            if (section == null) {
                section = PS.get().worlds.createSection("worlds.*");
            }
            newArea.saveConfiguration(section);
            newArea.loadDefaultConfiguration(section);
            area.foreachPlotAbs(new com.intellectualcrafters.plot.object.RunnableVal<Plot>() {
                @Override
                public void run(Plot plot) {
                    plot.setArea(newArea);
                }
            });
        }
        DBFunc.dbManager = new DelegateDB(DBFunc.dbManager, this);
    }

    public void teleport(FawePlayer fp, Server server, Plot plot) {
        if (server != null && server.getId() != Settings.IMP.SERVER_ID) {
            RedEdit.imp().teleport(fp, server.getName());
            // player, world, areaid, idString
            String[] arg = new String[]{fp.getName(), plot.getWorldName(), plot.getArea().id, plot.getId().toString()};
            this.teleport.call(server.getChannel().getGroup(), server.getId(), arg);
        } else {
            plot.teleportPlayer(PlotPlayer.wrap(fp.getName()));
        }
    }

    public Server load(String world) {
        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            SinglePlotArea area = ((SinglePlotAreaManager) manager).getArea();
            if (area != null) {
                PlotId id = PlotId.fromString(world);
                if (id != null) {
                    NetworkPlot plot = (NetworkPlot) area.getOwnedPlot(id);
                    if (plot == null) {
                        return null;
                    }
                    if (plot.isLoaded()) {
                        return RedEdit.get().getNetwork().getLocalServer();
                    }
                    try {
                        claims.put(world, System.nanoTime());
                        Server claimedServer = getClaimedServer(world);
                        if (claimedServer != null) {
                            if (claimedServer == RedEdit.get().getNetwork().getLocalServer()) {
                                area.loadWorld(id);
                            }
                            return claimedServer;
                        }
                    } finally {
                        claims.invalidate(world);
                    }
                }
            }
        }
        return null;
    }

    public Map.Entry<Server, Boolean> getLoaded(String world, int groupId) {
        if (groupId == Settings.IMP.SERVER_GROUP) {
            if (WorldUtil.IMP.isWorld(world)) {
                Server server = RedEdit.get().getScheduler().getLocalServer();
                return new AbstractMap.SimpleEntry<>(server, true);
            }
        }
        return this.isLoaded.any(groupId, 0, world, new ResultCall<Boolean>() {
            @Override
            public boolean add(Server server, Boolean result) {
                if (result) {
                    super.add(server, result);
                    return true;
                }
                return false;
            }
        });
    }

    public Server getClaimed(String world, int groupId) {
        if (groupId == Settings.IMP.SERVER_GROUP) {
            if (WorldUtil.IMP.isWorld(world)) {
                Server server = RedEdit.get().getScheduler().getLocalServer();
                return server;
            }
        }
        Map<Server, Long> allClaims = this.getClaimed.collect(groupId, 0, world);
        if (allClaims.isEmpty()) return null;
        long lowest = Long.MAX_VALUE;
        Server lowestServer = null;
        for (Map.Entry<Server, Long> entry : allClaims.entrySet()) {
            long time = entry.getValue();
            if (time < lowest) {
                lowest = time;
                lowestServer = entry.getKey();
            }
        }
        return lowestServer;
    }

    public Server getClaimedServer(String world) {
        return getClaimed(world, Settings.IMP.SERVER_GROUP);
    }

    public Server getLoadedServer(String world) {
        return getLoadedServer(world, Settings.IMP.SERVER_GROUP);
    }

    public Server getLoadedServer(String world, int groupId) {
        Map.Entry<Server, Boolean> loaded = getLoaded(world, groupId);
        return loaded != null ? loaded.getKey() : null;
    }

    public boolean isLoaded(String world) {
        return isLoaded(world, Settings.IMP.SERVER_GROUP);
    }

    public boolean isLoaded(String world, int groupId) {
        Map.Entry<Server, Boolean> loaded = getLoaded(world, groupId);
        return loaded != null && loaded.getValue();
    }

    public void disableTeleport(FawePlayer fp) {
        PlotPlayer.wrap(fp.getName()).setMeta("teleportOnLogin", false);
    }

    public void syncPlot(Plot plot) {
        if (plot.getArea() == null) {
            return;
        }
        syncPlot.call(Settings.IMP.SERVER_GROUP, 0, new PlotData(plot));
    }

    public void deletePlot(Plot plot) {
        deletePlot.call(Settings.IMP.SERVER_GROUP, 0, new PlotData(plot));
    }
}