package com.boydti.rededit.command.teleport;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.util.M;
import com.boydti.rededit.util.RedUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.command.parametric.Optional;

public class TeleportCommands {

    private final RedUtil util;

    public TeleportCommands(RedUtil util) {
        this.util = util;
    }

    @Command(
            aliases = { "/tptoggle" },
            usage = "[player]",
            desc = "Toggle teleport requests",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tptoggle")
    public void tptoggle(Player player, String other) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (other != null) {
            M.TOGGLE_TELEPORT_SPECIFIC.send(fp, other, fp.toggle("rededit.tp.disabled." + other));
        } else {
            M.TOGGLE_TELEPORT.send(fp, fp.toggle("rededit.tp.disabled"));
        }
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
        if (!RedEdit.get().getServerController().isOnline(other)) {
            player.print(M.getPrefix() + BBC.PLAYER_NOT_FOUND.format(other));
            return;
        }
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
            aliases = { "/tpaccept" },
            usage = "[player]",
            desc = "Accept a teleport request",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.tpaccept")
    public void tpaccept(Player player, @Optional("") String other) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        TeleportRequest request = util.getRequest(fp, other);
        if (request == null) {
            M.NO_REQUEST_FOUND.send(fp);
        } else {
            if (!RedEdit.get().getServerController().isOnline(other)) {
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
        if (!RedEdit.get().getServerController().isOnline(other)) {
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
        if (!RedEdit.get().getServerController().isOnline(other)) {
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
            M.TELEPORTING.send(fp);
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
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.home")
    public void home(Player player, String other) throws WorldEditException {

    }

    @Command(
            aliases = { "/sethome" },
            usage = "[home]",
            desc = "Set your home location",
            min = 1,
            max = 1
    )
    @CommandPermissions("rededit.sethome")
    public void sethome(Player player, String other) throws WorldEditException {

    }

    @Command(
            aliases = { "/setwarp" },
            usage = "[warpname]",
            desc = "Add a warp",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.setwarp")
    public void setwarp(Player player, String other) throws WorldEditException {

    }

    @Command(
            aliases = { "/warp" },
            usage = "[warp]",
            desc = "Warp to a location",
            min = 0,
            max = 1
    )
    @CommandPermissions("rededit.warp")
    public void warp(Player player, String other) throws WorldEditException {

    }
}
