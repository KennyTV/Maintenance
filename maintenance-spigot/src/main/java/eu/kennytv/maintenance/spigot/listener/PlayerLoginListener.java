package eu.kennytv.maintenance.spigot.listener;

import eu.kennytv.maintenance.spigot.MaintenanceSpigotPlugin;
import eu.kennytv.maintenance.spigot.SettingsSpigot;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerLoginListener implements Listener {
    private final MaintenanceSpigotPlugin plugin;
    private final SettingsSpigot settings;
    private final Set<UUID> notifiedPlayers = new HashSet<>();
    private final UUID notifyUuid = new UUID(-6334418481592579467L, -4779835342378829761L);

    public PlayerLoginListener(final MaintenanceSpigotPlugin plugin, final SettingsSpigot settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void postLogin(final PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (player.getUniqueId().equals(notifyUuid))
            player.sendMessage("§6MaintenanceSpigot §aVersion " + plugin.getVersion());
        else if (settings.isMaintenance()) {
            if (!player.hasPermission("maintenance.bypass") && !settings.getWhitelistedPlayers().containsKey(player.getUniqueId())) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(settings.getKickMessage().replace("%NEWLINE%", "\n"));

                if (settings.isJoinNotifications())
                    plugin.getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission("maintenance.joinnotification"))
                            .forEach(p -> p.sendMessage(settings.getMessage("joinNotification").replace("%PLAYER%", player.getName())));
                return;
            }
        }

        if (!player.hasPermission("maintenance.admin") || notifiedPlayers.contains(player.getUniqueId())) return;

        plugin.async(() -> {
            if (!plugin.updateAvailable()) return;
            player.sendMessage(plugin.getPrefix() + "§cThere is a newer version available: §aVersion " + plugin.getNewestVersion() + "§c, you're on §a" + plugin.getVersion());
            notifiedPlayers.add(player.getUniqueId());

            try {
                final TextComponent tc1 = new TextComponent(TextComponent.fromLegacyText(plugin.getPrefix()));
                final TextComponent tc2 = new TextComponent(TextComponent.fromLegacyText("§cDownload it at: §6https://www.spigotmc.org/resources/maintenancemode.40699/"));
                final TextComponent click = new TextComponent(TextComponent.fromLegacyText(" §7§l§o(CLICK ME)"));
                click.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/maintenancemode.40699/"));
                click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aDownload the latest version").create()));
                tc1.addExtra(tc2);
                tc1.addExtra(click);

                player.spigot().sendMessage(tc1);
            } catch (final Exception e) {
                player.sendMessage(plugin.getPrefix() + "§cDownload it at: §6https://www.spigotmc.org/resources/maintenancemode.40699/");
            }
        });
    }
}
