package com.boydti.rededit.util.plot;

import com.boydti.rededit.RedEdit;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.flag.Flag;
import com.github.intellectualsites.plotsquared.plot.flag.FlagManager;
import com.github.intellectualsites.plotsquared.plot.object.BlockLoc;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotId;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class PlotData implements Serializable {
    public int px, py, pz;
    public float yaw, pitch;
    public int temp;
    public int x, y;
    public String worldName;
    public String areaId;
    public UUID owner;
    public long timestamp;
    public HashSet<UUID> trusted;
    public HashSet<UUID> members;
    public HashSet<UUID> denied;
    public boolean[] merged;
    public String alias;
    public HashMap<String, String> flags;

    public PlotData() {}

    public PlotData(Plot plot) {
        this.temp = plot.temp;
        this.x = plot.getId().x;
        this.y = plot.getId().y;
        this.worldName = plot.getArea().worldname;
        this.areaId = plot.getArea().id;
        this.owner = plot.owner;
        this.timestamp = plot.getTimestamp();
        this.trusted = plot.getTrusted();
        this.members = plot.getMembers();
        this.denied = plot.getDenied();
        this.merged = plot.getSettings().merged;
        this.alias = plot.getSettings().alias;
        BlockLoc pos = plot.getSettings().getPosition();
        this.px = pos.getX();
        this.py = pos.getY();
        this.pz = pos.getZ();
        this.yaw = pos.getYaw();
        this.pitch = pos.getPitch();
        this.flags = new HashMap<>();
        for (Map.Entry<Flag<?>, Object> entry : plot.getSettings().flags.entrySet()) {
            Flag<?> flag = entry.getKey();
            flags.put(flag.getName(), flag.valueToString(entry.getValue()));
        }
    }

    public void load() {
        Plot plot = getPlot();
        HashMap<Flag<?>, Object> newFlags = new HashMap<Flag<?>, Object>();
        for (Map.Entry<String, String> entry : flags.entrySet()) {
            Flag flag = FlagManager.getFlag(entry.getKey());
            if (flag != null) {
                Object value = flag.parseValue(entry.getValue());
                newFlags.put(flag, value);
            } else {
                RedEdit.debug("Ignoring invalid flag: " + entry.getKey());
            }
        }
        BlockLoc pos = new BlockLoc(px, py, pz, yaw, pitch);
        if (plot != null) {
            plot.clearCache();
            plot.owner = owner;
//                plot.setTimestamp(timestamp);
            HashSet<UUID> trusted = plot.getTrusted();
            trusted.clear();
            trusted.addAll(this.trusted);

            HashSet<UUID> members = plot.getMembers();
            members.clear();
            members.addAll(this.members);

            HashSet<UUID> denied = plot.getDenied();
            denied.clear();
            denied.addAll(this.denied);

            plot.getSettings().merged = merged;
            plot.getSettings().alias = alias;
            plot.getSettings().setPosition(pos);
            plot.getSettings().flags = newFlags;

            plot.clearCache();
        } else {
            PlotArea area = PlotSquared.get().getPlotArea(worldName, areaId);
            if (area != null) {
                PlotId id = new PlotId(x, y);
                plot = new Plot(id, owner, trusted, members, denied, alias, pos, null, area, merged, timestamp, temp);
                plot.getSettings().flags = newFlags;
                Map<PlotId, Plot> map = area.getPlotsRaw();
                if (!area.addPlot(plot)) {
                    RedEdit.debug("Replacing plot: " + plot);
                }
            } else {
                RedEdit.debug("Unable to sync plot: " + worldName + (areaId != null ? ";" + areaId : "") + ";" + x + ";" + y);
            }
        }
    }

    public boolean remove() {
        PlotArea area = PlotSquared.get().getPlotArea(worldName, areaId);
        PlotId id = new PlotId(x, y);
        if (area != null) {
            Plot plot = area.getPlot(id);
            if (plot.temp == temp) {
                Map<PlotId, Plot> raw = area.getPlotsRaw();
                return raw.remove(id, plot);
            }
        }
        return false;
    }

    public Plot getPlot() {
        PlotArea area = PlotSquared.get().getPlotArea(worldName, areaId);
        PlotId id = new PlotId(x, y);
        if (area != null) {
            Plot plot = area.getPlot(id);
            if (plot != null && plot.temp == temp) {
                return plot;
            }
        }
        for (Plot plot : PlotSquared.get().getPlots()) {
            if (plot.temp == temp) {
                PlotArea newArea = PlotSquared.get().getPlotArea(worldName, areaId);
                Map<PlotId, Plot> plots = newArea.getPlotsRaw();
                PlotId currentId = plot.getId();
                if (newArea == area) {
                    plots.remove(currentId);
                    currentId.x = id.x;
                    currentId.y = id.y;
                    currentId.recalculateHash();
                    plots.put(id, plot);
                } else if (area != null) {
                    Map<PlotId, Plot> oldPlots = area.getPlotsRaw();
                    oldPlots.remove(currentId);
                    currentId.x = id.x;
                    currentId.y = id.y;
                    currentId.recalculateHash();
                    plots.put(id, plot);
                } else {
                    RedEdit.debug("Unable to find area: " + area + " to sync plot: " + plot);
                }
                return plot;
            }
        }
        if (area != null) {
            Plot plot = area.getPlot(id);
            if (plot != null && !plot.hasOwner()) {
                area.addPlot(plot);
                return plot;
            }
        }
        return null;
    }


}
