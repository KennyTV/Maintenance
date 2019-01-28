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

import eu.kennytv.maintenance.api.proxy.IMaintenanceProxy;
import eu.kennytv.maintenance.api.proxy.Server;
import eu.kennytv.maintenance.core.MaintenanceModePlugin;
import eu.kennytv.maintenance.core.Settings;
import eu.kennytv.maintenance.core.proxy.runnable.SingleMaintenanceRunnable;
import eu.kennytv.maintenance.core.runnable.MaintenanceRunnableBase;
import eu.kennytv.maintenance.core.util.ServerType;
import eu.kennytv.maintenance.core.util.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * @author KennyTV
 * @since 3.0
 */
public abstract class MaintenanceProxyPlugin extends MaintenanceModePlugin implements IMaintenanceProxy {
    private final Map<String, Task> serverTasks = new HashMap<>();
    protected SettingsProxy settings;

    protected MaintenanceProxyPlugin(final String version, final ServerType serverType) {
        super(version, serverType);
    }

    @Override
    public void setMaintenance(final boolean maintenance) {
        if (settings.getMySQL() != null) {
            settings.setMaintenanceToSQL(maintenance);
        } else {
            settings.setMaintenance(maintenance);
            settings.getConfig().set("enable-maintenance-mode", maintenance);
            settings.saveConfig();
        }

        serverActions(maintenance);
        if (isTaskRunning())
            cancelTask();
    }

    @Override
    public boolean isMaintenance(final Server server) {
        return settings.isMaintenance(server);
    }

    @Override
    public boolean setMaintenanceToServer(final Server server, final boolean maintenance) {
        if (maintenance) {
            if (!settings.addMaintenanceServer(server.getName())) return false;
        } else {
            if (!settings.removeMaintenanceServer(server.getName())) return false;
        }
        serverActions(server, maintenance);
        cancelSingleTask(server);
        return true;
    }

    @Override
    public boolean isServerTaskRunning(final Server server) {
        return serverTasks.containsKey(server.getName());
    }

    @Override
    protected String getPluginFolder() {
        return "plugins/";
    }

    public void cancelSingleTask(final Server server) {
        final Task task = serverTasks.remove(server.getName());
        if (task != null)
            task.cancel();
    }

    public MaintenanceRunnableBase startSingleMaintenanceRunnable(final Server server, final int minutes, final boolean enable) {
        final MaintenanceRunnableBase runnable = new SingleMaintenanceRunnable(this, (Settings) getSettings(), minutes, enable, server);
        serverTasks.put(server.getName(), startMaintenanceRunnable(runnable));
        return runnable;
    }

    protected abstract void serverActions(boolean maintenance);

    protected abstract void serverActions(Server server, boolean maintenance);
}