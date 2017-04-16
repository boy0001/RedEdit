package com.boydti.rededit.util.plot;

import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotSettings;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;

public class NetworkPlotArea extends SinglePlotArea {

    private final PlotLoader loader;

    public NetworkPlotArea(PlotLoader loader) {
        this.loader = loader;
    }

    public PlotLoader getPlotLoader() {
        return loader;
    }

    @Override
    protected Plot adapt(Plot p) {
        if (p instanceof NetworkPlot) {
            return p;
        }
        PlotSettings s = p.getSettings();
        p = new NetworkPlot(p.getId(), p.owner, p.getTrusted(), p.getMembers(), p.getDenied(), s.alias,  s.getPosition(), null, this, s.merged, p.getTimestamp(), p.temp);
        p.getSettings().flags = s.flags;
        return p;
    }

    @Override
    public Plot getPlotAbs(PlotId id) {
        Plot plot = getOwnedPlotAbs(id);
        if (plot == null) {
            return new NetworkPlot(this, id);
        }
        return plot;
    }

    @Override
    public Plot getPlot(PlotId id) {
        // TODO
        Plot plot = getOwnedPlotAbs(id);
        if (plot == null) {
            return new NetworkPlot(this, id);
        }
        return plot.getBasePlot(false);
    }
}
