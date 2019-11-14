package com.boydti.rededit.util.plot;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.sk89q.worldedit.entity.Player;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.Server;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.flag.Flag;
import com.github.intellectualsites.plotsquared.plot.generator.GeneratorWrapper;
import com.github.intellectualsites.plotsquared.plot.object.BlockLoc;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotId;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.worlds.SinglePlot;
import com.github.intellectualsites.plotsquared.plot.object.worlds.SingleWorldGenerator;
import com.github.intellectualsites.plotsquared.plot.util.WorldUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

public class NetworkPlot extends SinglePlot {

    public NetworkPlot(PlotArea area, PlotId id, UUID owner) {
        super(area, id, owner);
    }

    public NetworkPlot(PlotArea area, PlotId id) {
        super(area, id);
    }

    public NetworkPlot(PlotArea area, PlotId id, UUID owner, int temp) {
        super(area, id, owner, temp);
    }

    public NetworkPlot(PlotId id, UUID owner, HashSet<UUID> trusted, HashSet<UUID> members, HashSet<UUID> denied, String alias, BlockLoc position, Collection<Flag> flags, PlotArea area, boolean[] merged, long timestamp, int temp) {
        super(id, owner, trusted, members, denied, alias, position, flags, area, merged, timestamp, temp);
    }

    @Override
    public NetworkPlotArea getArea() {
        return (NetworkPlotArea) super.getArea();
    }

    @Override
    public boolean teleportPlayer(PlotPlayer player) {
        if (isLoaded() && Fawe.isMainThread()) {
            return super.teleportPlayer(player);
        }
        if (!player.isOnline()) return false;
        Supplier<Boolean> task = () -> {
            PlotLoader loader = getArea().getPlotLoader();
            String world = getWorldName();
            Server server = loader.getClaimedServer(world);
            if (server == null) {
                server = loader.load(world);
            }
            if (server == null) {
                player.sendMessage(Captions.PREFIX + "Failed to load plot, please try again.");
                return false;
            }
            final Plot thisPlot = this;
            if (server.getId() == Settings.IMP.SERVER_ID) {
                if (!isLoaded()) {
                    String worldName = getId().toCommaSeparatedString();
                    GeneratorWrapper<ChunkGenerator> generator = new SingleWorldGenerator().<ChunkGenerator>specify(worldName);
                    WorldCreator creator = new WorldCreator(worldName)
                    .generator(generator.getPlatformGenerator())
                    .environment(World.Environment.NORMAL)
                    .type(WorldType.FLAT);

                    AsyncWorld created = AsyncWorld.create(creator);
                }
                TaskManager.IMP.task(() -> super.teleportPlayer(player));
            } else {
                Player fp = Fawe.imp().wrap(player.getName());
                loader.teleport(fp, server, this);
            }
            return true;
        };
        if (Fawe.isMainThread()) {
            TaskManager.IMP.taskNow(() -> task.get(), true);
        } else {
            return task.get();
        }
        return true;
    }

    @Override
    public boolean isLoaded() {
        return WorldUtil.IMP.isWorld(getWorldName());
    }
}
