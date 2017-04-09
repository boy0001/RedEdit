package com.boydti.rededit.util;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;

public class PlotLoader {
    public PlotLoader() {
        PS.get();
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
}
