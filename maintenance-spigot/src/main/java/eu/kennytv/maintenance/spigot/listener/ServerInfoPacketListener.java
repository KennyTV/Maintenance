package eu.kennytv.maintenance.spigot.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import eu.kennytv.maintenance.core.listener.IPingListener;
import eu.kennytv.maintenance.spigot.MaintenanceSpigotBase;
import eu.kennytv.maintenance.spigot.SettingsSpigot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ServerInfoPacketListener extends PingListenerBase {
    //private WrappedServerPing.CompressedImage image;

    public ServerInfoPacketListener(final MaintenanceSpigotBase base, final SettingsSpigot settings) {
        super(base, settings);
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(base, ListenerPriority.HIGHEST,
                PacketType.Status.Server.SERVER_INFO) {
            @Override
            public void onPacketSending(final PacketEvent event) {
                if (!settings.isMaintenance()) return;

                final WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                ping.setMotD(settings.getRandomPingMessage());

                if (settings.hasCustomPlayerCountMessage()) {
                    ping.setVersionProtocol(0);
                    ping.setVersionName(settings.getPlayerCountMessage()
                            .replace("%ONLINE%", Integer.toString(base.getServer().getOnlinePlayers().size()))
                            .replace("%MAX%", Integer.toString(base.getServer().getMaxPlayers())));
                }

                final List<WrappedGameProfile> players = new ArrayList<>();
                for (final String string : settings.getPlayerCountHoverMessage().split("%NEWLINE%"))
                    players.add(new WrappedGameProfile(UUID.randomUUID(), string));
                ping.setPlayers(players);
                //if (settings.hasCustomIcon() && image != null) ping.setFavicon(image);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void serverListPing(final ServerListPingEvent event) {
        if (settings.isMaintenance() && settings.hasCustomIcon() && serverIcon != null)
            event.setServerIcon(serverIcon);
    }

    /*@Override
    public boolean loadIcon() {
        try {
            image = WrappedServerPing.CompressedImage.fromPng(ImageIO.read(new File("maintenance-icon.png")));
        } catch (final Exception e) {
            pl.getLogger().warning("Could not load 'maintenance-icon.png' - did you create one in your Spigot folder (not the plugins folder)?");
            if (pl.getApi().getSettings().debugEnabled())
                e.printStackTrace();
            return false;
        }
        return true;
    }*/
}
