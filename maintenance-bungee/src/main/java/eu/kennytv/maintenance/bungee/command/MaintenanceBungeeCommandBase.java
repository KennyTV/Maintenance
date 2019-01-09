package eu.kennytv.maintenance.bungee.command;

import eu.kennytv.maintenance.bungee.util.ProxiedSenderInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public final class MaintenanceBungeeCommandBase extends Command {
    private final MaintenanceBungeeCommand command;

    public MaintenanceBungeeCommandBase(final MaintenanceBungeeCommand command) {
        super("maintenance", "", "maintenancebungee");
        this.command = command;
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        command.execute(new ProxiedSenderInfo(sender), args);
    }
}
