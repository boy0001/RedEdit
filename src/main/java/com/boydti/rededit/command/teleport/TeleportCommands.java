package com.boydti.rededit.command.teleport;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.StringMan;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.config.UserConf;
import com.boydti.rededit.config.WarpConf;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.remote.Position;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.util.M;
import com.boydti.rededit.util.TeleportUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.command.parametric.Optional;

public class TeleportCommands {

    private final TeleportUtil util;
    private final Network controller;

    public TeleportCommands(TeleportUtil util, Network controller) {
        this.util = util;
        this.controller = controller;
    }

    @Command(
            aliases = { "/tptoggle" },
            usage = "[player]",
            desc = "Toggle teleport requests",
            min = 0,
            max = 0
    )
    @CommandPermissions("rededit.tptoggle")
    public void tptoggle(Player player) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
//        if (other != null) {
//            M.TOGGLE_TELEPORT_SPECIFIC.send(fp, other, fp.toggle("rededit.tp.disabled." + other));
//        } else {
            M.TOGGLE_TELEPORT.send(fp, fp.toggle("rededit.tp.disabled"));
//        }
    }

    @Command(
            aliases = { "/tpa" },
            usage = "<player>",
            desc = "Send a teleport request to a player",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.tpa")
    public void tpa(Player player, String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        System.out.println("Player found " + controller.getServer(other));
        TeleportRequest request = new TeleportRequest(player.getName(), other, true, player.hasPermission("rededit.tp.override"));
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        util.tpa(request, new RunnableVal2<Server, TPAResponse>() {
            @Override
            public void run(Server server, TPAResponse response) {
                switch (response) {
                    case ALLOW:
                        M.TPA_ALLOWED.send(fp, other);
                        break;
                    case DUPLICATE:
                        M.TPA_DUPLICATE.send(fp, other);
                        break;
                    case DENY:
                        M.TPA_DENIED.send(fp, other);
                        break;
                }
            }
        });
    }

    @Command(
            aliases = { "/tpdeny" },
            usage = "[player]",
            desc = "Reject a teleport request",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tpdeny")
    public void tpdeny(Player player, @Optional("") String other) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        TeleportRequest request = util.removeRequest(fp, other);
        if (request == null) {
            M.NO_REQUEST_FOUND.send(fp);
        } else {
            M.TPA_REJECTED.send(fp, request.sender);
        }
    }

    @Command(
            aliases = { "/tpaccept" },
            usage = "[player]",
            desc = "Accept a teleport request",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tpaccept")
    public void tpaccept(Player player, @Optional("") String other) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        TeleportRequest request = util.removeRequest(fp, other);
        if (request == null) {
            M.NO_REQUEST_FOUND.send(fp);
        } else {
            other = request.sender;
            if (!controller.isOnline(other)) {
                player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
                return;
            }
            M.TPA_ACCEPTED.send(fp, request.sender);
            util.sendMessage(other, M.getPrefix() + M.TPA_ACCEPTED_SENDER.format(fp.getName()));
            if (request.to) {
                util.teleport(other, fp);
            } else {
                util.teleport(fp, other);
            }
        }
    }

    @Command(
            aliases = { "/tp" },
            usage = "[player]",
            desc = "Accept a teleport request",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tp")
    public void tp(Player player, String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        M.TELEPORTING.send(fp, other);
        util.teleport(fp, other);
    }

    @Command(
            aliases = { "/tphere" },
            usage = "[player]",
            desc = "Accept a teleport request",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tphere")
    public void tphere(Player player, String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        M.TELEPORTING.send(fp, other);
        util.teleport(other, fp);
    }

    @Command(
            aliases = { "/back" },
            desc = "Teleport to your previous location",
            min = 0,
            max = 0
    )
    @CommandPermissions("rededit.back")
    public void back(Player player) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (!util.back(fp)) {
            M.NO_BACK.send(fp);
        } else {
            M.TELEPORTING.send(fp, "BACK");
        }
    }

//    @Command(
//            aliases = { "/tppos" },
//            usage = "[position]",
//            desc = "Accept a teleport request",
//            min = 0,
//            max = 1
//    )
//    @CommandPermissions("rededit.tppos")
//    public void tppos(Player player, String other) throws WorldEditException {
//
//    }

    @Command(
            aliases = { "/home" },
            usage = "[home]",
            desc = "Teleport to your home",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.home")
    public void home(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOME home = conf.getHome(name);
        if (home == null) {
            M.HOME_NOT_FOUND.send(fp, StringMan.join(conf.HOME.getSections(), ", "));
        } else {
            Position pos = new Position(fp.getName(), home.WORLD, new Vector(home.X, home.Y, home.Z), home.SERVER);
            M.TELEPORTING.send(fp, name);
            util.teleport(fp, pos);
        }
    }

    @Command(
            aliases = { "/delhome" },
            usage = "[home]",
            desc = "Delete your home",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.delhome")
    public void delhome(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOME home = conf.getHome(name);
        if (home == null) {
            M.HOME_NOT_FOUND.send(fp, name);
        } else {
            conf.deleteHome(name);
            conf.save();
            M.HOME_DELETED.send(fp, name);
        }
    }

    @Command(
            aliases = { "/delwarp" },
            usage = "[warp]",
            desc = "Delete a warp",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.delwarp")
    public void delwarp(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        WarpConf warps = RedEdit.get().getWarpConfig();
        WarpConf.WARP warp = warps.getWarp(name);
        if (warp == null) {
            M.WARP_NOT_FOUND.send(fp, StringMan.join(warps.WARP.getSections(), ", "));
        } else {
            warps.deleteWarp(name);
            warps.save();
            M.WARP_DELETED.send(fp, name);
        }
    }

    @Command(
            aliases = { "/sethome" },
            usage = "[home]",
            desc = "Set your home location",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.sethome")
    public void sethome(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (!StringMan.isAlphanumericUnd(name)) {
            M.NOT_ALPHANUMERIC.send(fp, name);
            return;
        }
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOME home = new UserConf.HOME();
        WorldVector pos = player.getPosition();
        home.WORLD = Fawe.imp().getWorldName(pos.getWorld());
        home.SERVER = Settings.IMP.SERVER_ID;
        home.X = pos.getBlockX();
        home.Y = pos.getBlockY();
        home.Z = pos.getBlockZ();
        conf.addHome(name, home);
        conf.save();
        M.HOME_SET.send(fp, name);
    }

    @Command(
            aliases = { "/setwarp" },
            usage = "[warpname]",
            desc = "Add a warp",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.setwarp")
    public void setwarp(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (!StringMan.isAlphanumericUnd(name)) {
            M.NOT_ALPHANUMERIC.send(fp, name);
            return;
        }
        WarpConf conf = RedEdit.get().getWarpConfig();
        if (conf.getWarp(name) != null) {
            M.WARP_ALREADY_SET.send(fp, name);
            return;
        }
        WarpConf.WARP warp = new WarpConf.WARP();
        WorldVector pos = player.getPosition();
        warp.WORLD = Fawe.imp().getWorldName(pos.getWorld());
        warp.SERVER = Settings.IMP.SERVER_ID;
        warp.X = pos.getBlockX();
        warp.Y = pos.getBlockY();
        warp.Z = pos.getBlockZ();
        conf.addWarp(name, warp);
        conf.save();
        M.WARP_SET.send(fp, name);
    }

    @Command(
            aliases = { "/warp" },
            usage = "[warp]",
            desc = "Warp to a location",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.warp")
    public void warp(Player player, String name) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        WarpConf conf = RedEdit.get().getWarpConfig();
        WarpConf.WARP warp = conf.getWarp(name);
        if (warp == null) {
            M.WARP_NOT_FOUND.send(fp, name);
        } else {
            Position pos = new Position(fp.getName(), warp.WORLD, new Vector(warp.X, warp.Y, warp.Z), warp.SERVER);
            M.TELEPORTING.send(fp, name);
            util.teleport(fp, pos);
        }
    }
}
