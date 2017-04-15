package com.boydti.rededit.util.plot;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;

public class PlotLoader {

    private final RemoteCall<Object, PlotData> syncPlot;
    private final RemoteCall<Object, PlotData> deletePlot;

    public PlotLoader() {
        PS.get();
        this.syncPlot = new RemoteCall<Object, PlotData>() {
            @Override
            public Object run(Server sender, PlotData plot) {
                plot.load();
                return null;
            }
        };
        this.deletePlot = new RemoteCall<Object, PlotData>() {
            @Override
            public Object run(Server sender, PlotData arg) {
                arg.remove();
                return null;
            }
        };
        DBFunc.dbManager = new DelegateDB(DBFunc.dbManager, this);
    }

    public void load(String world) {
        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            SinglePlotArea area = ((SinglePlotAreaManager) manager).getArea();
            PlotId id = PlotId.fromString(world);
            if (id != null) {
                Plot plot = area.getOwnedPlot(id);
                if (plot != null) {
                    area.loadWorld(id);
                }
            }
        }
    }

    public void disableTeleport(FawePlayer fp) {
        PlotPlayer.wrap(fp.getName()).setMeta("teleportOnLogin", false);
    }

    public void syncPlot(Plot plot) {
        syncPlot.call(Settings.IMP.SERVER_GROUP, 0, new PlotData(plot));
    }

    public void deletePlot(Plot plot) {
        deletePlot.call(Settings.IMP.SERVER_GROUP, 0, new PlotData(plot));
    }
}