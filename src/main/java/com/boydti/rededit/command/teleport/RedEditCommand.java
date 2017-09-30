package com.boydti.rededit.command.teleport;

import com.boydti.rededit.RedEdit;
import com.boydti.rededit.util.M;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;

public class RedEditCommand {
    @Command(
            aliases = { "reload" },
            desc = "Reload the plugin"
    )
    @CommandPermissions("rededit.reload")
    public void reload(Player player) throws WorldEditException {
        RedEdit.get().setupConfig();
        player.print(M.getPrefix() + " Reloaded!");
    }
}
