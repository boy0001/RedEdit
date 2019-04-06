package com.boydti.rededit.util.plot;

import com.github.intellectualsites.plotsquared.plot.database.AbstractDB;
import com.github.intellectualsites.plotsquared.plot.flag.Flag;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotCluster;
import com.github.intellectualsites.plotsquared.plot.object.PlotId;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal;
import com.github.intellectualsites.plotsquared.plot.object.comment.PlotComment;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DelegateDB implements AbstractDB {
    private final AbstractDB db;
    private final PlotLoader loader;

    public DelegateDB(AbstractDB parent, PlotLoader loader) {
        this.db = parent;
        this.loader = loader;
    }

    @Override
    public void setOwner(Plot plot, UUID uuid) {
        db.setOwner(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void createPlotsAndData(List<Plot> list, Runnable runnable) {
        db.createPlotsAndData(list, runnable);
        for (Plot plot : list) {
            loader.syncPlot(plot);
        }
    }

    @Override
    public void createPlotSafe(Plot plot, Runnable runnable, Runnable runnable1) {
        db.createPlotSafe(plot, new Runnable() {
            @Override
            public void run() {
                loader.syncPlot(plot);
                if (runnable != null) {
                    runnable.run();
                }
            }
        }, runnable1);
    }

    @Override
    public void createTables() throws Exception {
        db.createTables();
    }

    @Override
    public void delete(Plot plot) {
        db.delete(plot);
        loader.deletePlot(plot);
    }

    @Override
    public void deleteSettings(Plot plot) {
        db.deleteSettings(plot);
    }

    @Override
    public void deleteHelpers(Plot plot) {
        db.deleteHelpers(plot);
    }

    @Override
    public void deleteTrusted(Plot plot) {
        db.deleteTrusted(plot);
    }

    @Override
    public void deleteDenied(Plot plot) {
        db.deleteDenied(plot);
    }

    @Override
    public void deleteComments(Plot plot) {
        db.deleteComments(plot);
    }

    @Override
    public void deleteRatings(Plot plot) {
        db.deleteRatings(plot);
    }

    @Override
    public void delete(PlotCluster plotCluster) {
        db.delete(plotCluster);
        // TODO
    }

    @Override
    public void addPersistentMeta(UUID uuid, String s, byte[] bytes, boolean b) {
        db.addPersistentMeta(uuid, s, bytes, b);
    }

    @Override
    public void removePersistentMeta(UUID uuid, String s) {
        db.removePersistentMeta(uuid, s);
    }

    @Override
    public void getPersistentMeta(UUID uuid, RunnableVal<Map<String, byte[]>> runnableVal) {
        db.getPersistentMeta(uuid, runnableVal);
    }

    @Override
    public void createPlotSettings(int i, Plot plot) {
        db.createPlotSettings(i, plot);
    }

    @Override
    public int getId(Plot plot) {
        return db.getId(plot);
    }

    @Override
    public int getClusterId(PlotCluster plotCluster) {
        return db.getClusterId(plotCluster);
    }

    @Override
    public HashMap<String, HashMap<PlotId, Plot>> getPlots() {
        return db.getPlots();
    }

    @Override
    public void validateAllPlots(Set<Plot> set) {
        db.validateAllPlots(set);
    }

    @Override
    public HashMap<String, Set<PlotCluster>> getClusters() {
        return db.getClusters();
    }

    @Override
    public void setMerged(Plot plot, boolean[] booleen) {
        db.setMerged(plot, booleen);
        loader.syncPlot(plot);
    }

    @Override
    public void swapPlots(Plot plot, Plot plot1) {
        db.swapPlots(plot, plot1);
        loader.syncPlot(plot);
        loader.syncPlot(plot1);
    }

    @Override
    public void setFlags(Plot plot, HashMap<Flag<?>, Object> hashMap) {
        db.setFlags(plot, hashMap);
        loader.syncPlot(plot);
    }

    @Override
    public void setClusterName(PlotCluster plotCluster, String s) {
        db.setClusterName(plotCluster, s);
        // TODO
    }

    @Override
    public void setAlias(Plot plot, String s) {
        db.setAlias(plot, s);
        loader.syncPlot(plot);
    }

    @Override
    public void purgeIds(Set<Integer> set) {
        db.purgeIds(set);
        // TODO
    }

    @Override
    public void purge(PlotArea plotArea, Set<PlotId> set) {
        db.purge(plotArea, set);
        // TODO
    }

    @Override
    public void setPosition(Plot plot, String s) {
        db.setPosition(plot, s);
        loader.syncPlot(plot);
    }

    @Override
    public void setPosition(PlotCluster plotCluster, String s) {
        db.setPosition(plotCluster, s);
        // TODO
    }

    @Override
    public void removeTrusted(Plot plot, UUID uuid) {
        db.removeTrusted(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void removeHelper(PlotCluster plotCluster, UUID uuid) {
        db.removeHelper(plotCluster, uuid);
    }

    @Override
    public void removeMember(Plot plot, UUID uuid) {
        db.removeMember(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void removeInvited(PlotCluster plotCluster, UUID uuid) {
        db.removeInvited(plotCluster, uuid);
        // TODO
    }

    @Override
    public void setTrusted(Plot plot, UUID uuid) {
        db.setTrusted(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void setHelper(PlotCluster plotCluster, UUID uuid) {
        db.setHelper(plotCluster, uuid);
        //  TODO
    }

    @Override
    public void setMember(Plot plot, UUID uuid) {
        db.setMember(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void setInvited(PlotCluster plotCluster, UUID uuid) {
        db.setInvited(plotCluster, uuid);
        // TODO
    }

    @Override
    public void removeDenied(Plot plot, UUID uuid) {
        db.removeDenied(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public void setDenied(Plot plot, UUID uuid) {
        db.setDenied(plot, uuid);
        loader.syncPlot(plot);
    }

    @Override
    public HashMap<UUID, Integer> getRatings(Plot plot) {
        return db.getRatings(plot);
    }

    @Override
    public void setRating(Plot plot, UUID uuid, int i) {
        db.setRating(plot, uuid, i);
        loader.syncPlot(plot);
    }

    @Override
    public void removeComment(Plot plot, PlotComment plotComment) {
        db.removeComment(plot, plotComment);
        loader.syncPlot(plot);
    }

    @Override
    public void clearInbox(Plot plot, String s) {
        db.clearInbox(plot, s);
        loader.syncPlot(plot);
    }

    @Override
    public void setComment(Plot plot, PlotComment plotComment) {
        db.setComment(plot, plotComment);
        loader.syncPlot(plot);
    }

    @Override
    public void getComments(@Nonnull Plot plot, String s, RunnableVal<List<PlotComment>> runnableVal) {
        db.getComments(plot, s, runnableVal);
    }

    @Override
    public void createPlotAndSettings(Plot plot, Runnable runnable) {
        db.createPlotAndSettings(plot, new Runnable() {
            @Override
            public void run() {
                loader.syncPlot(plot);
                runnable.run();
            }
        });
    }

    @Override
    public void createCluster(PlotCluster plotCluster) {
        db.createCluster(plotCluster);
        // TODO
    }

    @Override
    public void resizeCluster(PlotCluster plotCluster, PlotId plotId, PlotId plotId1) {
        db.resizeCluster(plotCluster, plotId, plotId1);
        // TODO
    }

    @Override
    public void movePlot(Plot plot, Plot plot1) {
        db.movePlot(plot, plot1);
        loader.syncPlot(plot);
        loader.syncPlot(plot1);
    }

    @Override
    public void replaceUUID(UUID uuid, UUID uuid1) {
        db.replaceUUID(uuid, uuid1);
    }

    @Override
    public boolean deleteTables() {
        return db.deleteTables();
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void replaceWorld(String s, String s1, PlotId plotId, PlotId plotId1) {
        db.replaceWorld(s, s1, plotId, plotId1);
    }

    @Override
    public void updateTables(int[] ints) {
        db.updateTables(ints);
    }
}
