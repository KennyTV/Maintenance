/*
 * Maintenance - https://git.io/maintenancemode
 * Copyright (C) 2018 KennyTV (https://github.com/KennyTV)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.kennytv.maintenance.core.proxy;

import eu.kennytv.maintenance.api.event.proxy.ServerMaintenanceChangedEvent;
import eu.kennytv.maintenance.api.proxy.IMaintenanceProxy;
import eu.kennytv.maintenance.api.proxy.Server;
import eu.kennytv.maintenance.core.MaintenancePlugin;
import eu.kennytv.maintenance.core.proxy.command.MaintenanceProxyCommand;
import eu.kennytv.maintenance.core.proxy.runnable.SingleMaintenanceRunnable;
import eu.kennytv.maintenance.core.proxy.server.DummyServer;
import eu.kennytv.maintenance.core.runnable.MaintenanceRunnableBase;
import eu.kennytv.maintenance.core.util.SenderInfo;
import eu.kennytv.maintenance.core.util.ServerType;
import eu.kennytv.maintenance.core.util.Task;

import java.util.*;

/**
 * @author KennyTV
 * @since 3.0
 */
public abstract class MaintenanceProxyPlugin extends MaintenancePlugin implements IMaintenanceProxy {
    private final Map<String, Task> serverTasks = new HashMap<>();
    protected SettingsProxy settingsProxy;

    protected MaintenanceProxyPlugin(final String version, final ServerType serverType) {
        super(version, serverType);
    }

    @Override
    public void disable() {
        super.disable();
        if (settingsProxy.getMySQL() != null) {
            settingsProxy.getMySQL().close();
        }
    }

    @Override
    public void setMaintenance(final boolean maintenance) {
        if (settingsProxy.hasMySQL()) {
            settingsProxy.setMaintenanceToSQL(maintenance);
        }
        super.setMaintenance(maintenance);
    }

    @Override
    public boolean isMaintenance(final Server server) {
        return settingsProxy.isMaintenance(server.getName());
    }

    @Override
    public boolean setMaintenanceToServer(final Server server, final boolean maintenance) {
        if (maintenance) {
            if (!settingsProxy.addMaintenanceServer(server.getName())) return false;
        } else {
            if (!settingsProxy.removeMaintenanceServer(server.getName())) return false;
        }
        serverActions(server, maintenance);
        return true;
    }

    public void serverActions(final Server server, final boolean maintenance) {
        if (server == null || server instanceof DummyServer) return;
        if (maintenance) {
            final Server fallback = getServer(settingsProxy.getFallbackServer());
            if (fallback == null) {
                if (server.hasPlayers())
                    getLogger().warning("The set fallback could not be found! Instead kicking players from that server off the network!");
            } else if (fallback.getName().equals(server.getName()))
                getLogger().warning("Maintenance has been enabled on the fallback server! If a player joins on a proxied server, they will be kicked completely instead of being sent to the fallback server!");
            kickPlayers(server, fallback);
        } else
            server.broadcast(settingsProxy.getMessage("singleMaintenanceDeactivated").replace("%SERVER%", server.getName()));

        cancelSingleTask(server);
        eventManager.callEvent(new ServerMaintenanceChangedEvent(server, maintenance));
    }

    @Override
    public boolean isServerTaskRunning(final Server server) {
        return serverTasks.containsKey(server.getName());
    }

    @Override
    public Set<String> getMaintenanceServers() {
        return Collections.unmodifiableSet(settingsProxy.getMaintenanceServers());
    }

    public void cancelSingleTask(final Server server) {
        final Task task = serverTasks.remove(server.getName());
        if (task != null)
            task.cancel();
    }

    public MaintenanceRunnableBase startSingleMaintenanceRunnable(final Server server, final int minutes, final boolean enable) {
        final MaintenanceRunnableBase runnable = new SingleMaintenanceRunnable(this, settingsProxy, minutes * 60, enable, server);
        serverTasks.put(server.getName(), runnable.getTask());
        return runnable;
    }

    @Override
    public List<String> getMaintenanceServersDump() {
        final List<String> list = new ArrayList<>();
        if (isMaintenance()) list.add("global");
        list.addAll(settingsProxy.getMaintenanceServers());
        return list.isEmpty() ? null : list;
    }

    @Override
    public MaintenanceProxyCommand getCommandManager() {
        return (MaintenanceProxyCommand) commandManager;
    }

    public SettingsProxy getSettingsProxy() {
        return settingsProxy;
    }

    public abstract String getServer(SenderInfo sender);

    protected abstract void kickPlayers(Server server, Server fallback);
}