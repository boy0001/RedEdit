package com.boydti.rededit.util.plot;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.remote.Server;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.generator.GeneratorWrapper;
import com.intellectualcrafters.plot.object.BlockLoc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.worlds.SinglePlot;
import com.intellectualcrafters.plot.object.worlds.SingleWorldGenerator;
import com.intellectualcrafters.plot.util.WorldUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
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
        Runnable task = () -> {
            PlotLoader loader = getArea().getPlotLoader();
            String world = getWorldName();
            Server server = loader.getClaimedServer(world);
            if (server == null) {
                server = loader.load(world);
            }
            if (server == null) {
                player.sendMessage(C.PREFIX + "Failed to load plot, please try again.");
                return;
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
                FawePlayer<Object> fp = FawePlayer.wrap(player.getName());
                loader.teleport(fp, server, this);
            }
        };
        if (Fawe.isMainThread()) {
            TaskManager.IMP.taskNow(task, true);
        } else {
            task.run();
        }
        return true;
    }

    @Override
    public boolean isLoaded() {
        return WorldUtil.IMP.isWorld(getWorldName());
    }
}
