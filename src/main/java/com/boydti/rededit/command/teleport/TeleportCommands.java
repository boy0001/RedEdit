package com.boydti.rededit.command.teleport;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.entity.Player;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.StringMan;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.config.UserConf;
import com.boydti.rededit.config.WarpConf;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.remote.Group;
import com.boydti.rededit.remote.Position;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.VoidSerializer;
import com.boydti.rededit.util.M;
import com.boydti.rededit.util.TeleportUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class TeleportCommands {

    private final TeleportUtil util;
    private final Network controller;
    private final RemoteCall refreshWarps;

    public TeleportCommands(TeleportUtil util, Network controller) {
        this.util = util;
        this.controller = controller;

        this.refreshWarps = new RemoteCall<Object, Object>() {
            @Override
            public Object run(Server sender, Object arg) {
                RedEdit.get().unloadWarps();
                return null;
            }
        }.setSerializer(new VoidSerializer(), new VoidSerializer());
    }

    @Command(
            name = "/tptoggle",
            desc = "Toggle teleport requests"
    )
    @CommandPermissions("rededit.tptoggle")
    public void tptoggle(Player player) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
//        if (other != null) {
//            M.TOGGLE_TELEPORT_SPECIFIC.send(fp, other, fp.toggle("rededit.tp.disabled." + other));
//        } else {
            M.TOGGLE_TELEPORT.send(fp, fp.toggle("rededit.tp.disabled"));
//        }
    }

    @Command(
            name =  "/tpa" ,
            desc = "Send a teleport request to a player"
    )
    @CommandPermissions("rededit.tpa")
    public void tpa(Player player, @Arg(desc = "Player name") String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        TeleportRequest request = new TeleportRequest(player.getName(), other, true, player.hasPermission("rededit.tp.override"));
        Player fp = Fawe.imp().wrap(player);
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
            name = "/tpahere",
            desc = "Send a teleport request to a player"
    )
    @CommandPermissions("rededit.tpahere")
    public void tpahere(Player player, @Arg(desc = "Player name") String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        TeleportRequest request = new TeleportRequest(player.getName(), other, false, player.hasPermission("rededit.tp.override"));
        Player fp = Fawe.imp().wrap(player);
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
            name = "/tpdeny",
            desc = "Reject a teleport request"
    )
    @CommandPermissions("rededit.tpdeny")
    public void tpdeny(Player player, @Arg(desc = "Player name", def="") String other) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        TeleportRequest request = util.removeRequest(fp, other);
        if (request == null) {
            M.NO_REQUEST_FOUND.send(fp);
        } else {
            M.TPA_REJECTED.send(fp, request.sender);
        }
    }

    @Command(
            name = "/tpaccept",
            desc = "Accept a teleport request"
    )
    @CommandPermissions("rededit.tpaccept")
    public void tpaccept(Player player, @Arg(desc = "Player name") String other) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
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
            name = "/tp",
            desc = "Accept a teleport request"
    )
    @CommandPermissions("rededit.tp")
    public void tp(Player player, String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        Player fp = Fawe.imp().wrap(player);
        M.TELEPORTING.send(fp, other);
        util.teleport(fp, other);
    }

    @Command(
            name = "/tphere",
            desc = "Accept a teleport request"
    )
    @CommandPermissions("rededit.tphere")
    public void tphere(Player player, String other) throws WorldEditException {
        if (!controller.isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
        Player fp = Fawe.imp().wrap(player);
        M.TELEPORTING.send(fp, other);
        util.teleport(other, fp);
    }

    @Command(
            name = "/back",
            desc = "Teleport to your previous location"
    )
    @CommandPermissions("rededit.back")
    public void back(Player player) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        if (!util.back(fp)) {
            M.NO_BACK.send(fp);
        } else {
            M.TELEPORTING.send(fp, "BACK");
        }
    }

    @Command(
            name = "/tpgroup",
            aliases = { "/tpg", "/teleportgroup" },
            desc = "Teleport to a random server in a group"
    )
    @CommandPermissions("rededit.back")
    public void group(Player player, int groupId) throws WorldEditException {
        Group group = controller.getGroup(groupId);
        if (group == null) {
            M.GROUP_NOT_FOUND.send(player, groupId);
            return;
        }
        Server server = group.getSmallestServer();
        if (server != null) {
            Player fp = Fawe.imp().wrap(player);
            M.TELEPORTING.send(fp, server.getName());
            server.teleportPlayer(fp);
        } else {
            M.GROUP_NOT_FOUND.send(player, groupId);
            return;
        }
    }

//    @Command(
//            name = "/tppos",
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
            name = "/home",
            desc = "Teleport to your home"
    )
    @CommandPermissions("rededit.home")
    public void home(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOMES home = conf.getHome(name);
        if (home == null) {
            M.HOME_NOT_FOUND.send(fp, StringMan.join(conf.HOMES.getSections(), ", "));
        } else {
            Position pos = new Position(fp.getName(), home.WORLD, Vector3.at(home.X, home.Y, home.Z), home.SERVER, home.GROUP);
            if (util.teleport(fp, pos)) {
                M.TELEPORTING.send(fp, name);
            } else {
                BBC.SELECTOR_INVALID_COORDINATES.send(fp, pos);
            }
        }
    }

    @Command(
            name = "/delhome",
            desc = "Delete your home"
    )
    @CommandPermissions("rededit.delhome")
    public void delhome(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOMES home = conf.getHome(name);
        if (home == null) {
            M.HOME_NOT_FOUND.send(fp, name);
        } else {
            conf.deleteHome(name);
            conf.save();
            M.HOME_DELETED.send(fp, name);
        }
    }

    @Command(
            name = "/delwarp",
            desc = "Delete a warp"
    )
    @CommandPermissions("rededit.delwarp")
    public void delwarp(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
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
            name = "/sethome",
            desc = "Set your home location"
    )
    @CommandPermissions("rededit.sethome")
    public void sethome(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        if (!StringMan.isAlphanumericUnd(name)) {
            M.NOT_ALPHANUMERIC.send(fp, name);
            return;
        }
        UserConf conf = RedEdit.get().getUserConf(player.getUniqueId());
        UserConf.HOMES home = new UserConf.HOMES();
        Location loc = player.getLocation();
        home.WORLD = Fawe.imp().getWorldName((World) loc.getExtent());
        home.SERVER = Settings.IMP.SERVER_ID;
        home.GROUP = Settings.IMP.SERVER_GROUP;
        home.X = loc.getBlockX();
        home.Y = loc.getBlockY();
        home.Z = loc.getBlockZ();
        conf.addHome(name, home);
        conf.save();
        M.HOME_SET.send(fp, name);
    }

    @Command(
            name = "/setwarp",
            desc = "Add a warp"
    )
    @CommandPermissions("rededit.setwarp")
    public void setwarp(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
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
        Location pos = player.getLocation();
        warp.WORLD = Fawe.imp().getWorldName((World) pos.getExtent());
        warp.SERVER = Settings.IMP.SERVER_ID;
        warp.GROUP = Settings.IMP.SERVER_GROUP;
        warp.X = pos.getBlockX();
        warp.Y = pos.getBlockY();
        warp.Z = pos.getBlockZ();
        conf.addWarp(name, warp);
        conf.save();
        refreshWarps.call(0, 0, true);
        M.WARP_SET.send(fp, name);
    }

    @Command(
            name = "/warp",
            desc = "Warp to a location"
    )
    @CommandPermissions("rededit.warp")
    public void warp(Player player, String name) throws WorldEditException {
        Player fp = Fawe.imp().wrap(player);
        WarpConf conf = RedEdit.get().getWarpConfig();
        WarpConf.WARP warp = conf.getWarp(name);
        if (warp == null) {
            M.WARP_NOT_FOUND.send(fp, StringMan.join(conf.WARP.getSections(), ", "));
        } else {
            Position pos = new Position(fp.getName(), warp.WORLD, Vector3.at(warp.X, warp.Y, warp.Z), warp.SERVER, warp.GROUP);
            if (util.teleport(fp, pos)) {
                M.TELEPORTING.send(fp, name);
            } else {
                BBC.SELECTOR_INVALID_COORDINATES.send(fp, pos);
            }
        }
    }
}
