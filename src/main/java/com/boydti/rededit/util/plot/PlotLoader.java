package com.boydti.rededit.util.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.ResultCall;
import com.boydti.rededit.remote.Server;
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

public class PlotLoader {

    private final RemoteCall<Object, PlotData> syncPlot;
    private final RemoteCall<Object, PlotData> deletePlot;
    private final RemoteCall<Boolean, String> isLoaded;
    private final RemoteCall<Object, String[]> teleport;

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
            public Boolean run(Server sender, String arg) {
                if (WorldUtil.IMP.isWorld(arg)) {
                    return true;
                }
                return false;
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

    public void load(String world) {
        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            SinglePlotArea area = ((SinglePlotAreaManager) manager).getArea();
            if (area != null) {
                PlotId id = PlotId.fromString(world);
                if (id != null) {
                    Plot plot = area.getOwnedPlot(id);
                    if (plot != null) {
                        area.loadWorld(id);
                    }
                }
            }
        }
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