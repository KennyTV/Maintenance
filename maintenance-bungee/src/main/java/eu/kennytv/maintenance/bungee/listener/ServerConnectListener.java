package eu.kennytv.maintenance.bungee.listener;

import eu.kennytv.maintenance.bungee.MaintenanceBungeePlugin;
import eu.kennytv.maintenance.bungee.SettingsBungee;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public final class ServerConnectListener implements Listener {
    private final MaintenanceBungeePlugin plugin;
    private final SettingsBungee settings;
    private boolean warned;

    public ServerConnectListener(final MaintenanceBungeePlugin plugin, final SettingsBungee settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void serverConnect(final ServerConnectEvent event) {
        final ProxiedPlayer p = event.getPlayer();
        final ServerInfo target = event.getTarget();
        if (!plugin.isMaintenance(target))return;
        if (p.hasPermission("maintenance.bypass") || settings.getWhitelistedPlayers().containsKey(p.getUniqueId()))
            return;

        event.setCancelled(true);
        if (settings.isJoinNotifications()) {
            target.getPlayers().stream().filter(player -> player.hasPermission("maintenance.joinnotification"))
                    .forEach(player -> player.sendMessage(settings.getMessage("joinNotification").replace("%PLAYER%", p.getName())));
        }
        // Normal serverconnect
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY && event.getReason() != ServerConnectEvent.Reason.KICK_REDIRECT
                && event.getReason() != ServerConnectEvent.Reason.LOBBY_FALLBACK && event.getReason() != ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT) {
            p.sendMessage(settings.getMessage("singleMaintenanceKick").replace("%SERVER%", target.getName()));
            return;
        }

        // If it's the initial proxy join or a kick from another server, go back to fallback server
        final ServerInfo fallback = plugin.getProxy().getServerInfo(settings.getFallbackServer());
        if (fallback == null || !fallback.canAccess(p) || plugin.isMaintenance(fallback)) {
            p.disconnect(settings.getMessage("singleMaintenanceKickComplete").replace("%NEWLINE%", "\n").replace("%SERVER%", target.getName()));
            if (!warned) {
                plugin.getLogger().warning("Could not send player to the fallback server set in the SpigotServers.yml! Instead kicking player off the network!");
                warned = true;
            }
        } else
            p.connect(fallback);
    }
}
