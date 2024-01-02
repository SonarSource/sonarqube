/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { findLastIndex } from 'lodash';
import { throwGlobalError } from '../helpers/error';
import { getJSON, post } from '../helpers/request';
import { isDefined } from '../helpers/types';
import {
  AvailablePlugin,
  InstalledPlugin,
  PendingPluginResult,
  PluginType,
  Update,
} from '../types/plugins';

export function getAvailablePlugins(): Promise<{
  plugins: AvailablePlugin[];
  updateCenterRefresh: string;
}> {
  return getJSON('/api/plugins/available').catch(throwGlobalError);
}

export function getPendingPlugins(): Promise<PendingPluginResult> {
  return getJSON('/api/plugins/pending').catch(throwGlobalError);
}

function getLastUpdates(updates: undefined | Update[]): Update[] {
  if (!updates) {
    return [];
  }
  const lastUpdate = ['COMPATIBLE', 'REQUIRES_SYSTEM_UPGRADE', 'DEPS_REQUIRE_SYSTEM_UPGRADE'].map(
    (status) => {
      const index = findLastIndex(updates, (update) => update.status === status);
      return index > -1 ? updates[index] : undefined;
    }
  );
  return lastUpdate.filter(isDefined);
}

function addChangelog(update: Update, updates?: Update[]) {
  if (!updates) {
    return update;
  }
  const index = updates.indexOf(update);
  const previousUpdates = index > 0 ? updates.slice(0, index) : [];
  return { ...update, previousUpdates };
}

function getInstalledPluginApi(type = PluginType.External) {
  return getJSON('/api/plugins/installed', { f: 'category', type });
}

function getUpdatesPluginApi() {
  return getJSON('/api/plugins/updates');
}

export function getInstalledPlugins(
  type: PluginType = PluginType.External
): Promise<InstalledPlugin[]> {
  return getInstalledPluginApi(type).then(({ plugins }) => plugins, throwGlobalError);
}

export function getInstalledPluginsWithUpdates(): Promise<InstalledPlugin[]> {
  return Promise.all([getInstalledPluginApi(), getUpdatesPluginApi()])
    .then(([installed, updates]) =>
      installed.plugins.map((plugin: InstalledPlugin) => {
        const updatePlugin: InstalledPlugin = updates.plugins.find(
          (p: InstalledPlugin) => p.key === plugin.key
        );
        if (updatePlugin) {
          return {
            ...updatePlugin,
            ...plugin,
            updates: getLastUpdates(updatePlugin.updates).map((update) =>
              addChangelog(update, updatePlugin.updates)
            ),
          };
        }
        return plugin;
      })
    )
    .catch(throwGlobalError);
}

export function getPluginUpdates(): Promise<InstalledPlugin[]> {
  return Promise.all([getUpdatesPluginApi(), getInstalledPluginApi()])
    .then(([updates, installed]) =>
      updates.plugins.map((updatePlugin: InstalledPlugin) => {
        const updates = getLastUpdates(updatePlugin.updates).map((update) =>
          addChangelog(update, updatePlugin.updates)
        );
        const plugin = installed.plugins.find((p: InstalledPlugin) => p.key === updatePlugin.key);
        if (plugin) {
          return {
            ...plugin,
            ...updatePlugin,
            updates,
          };
        }
        return { ...updatePlugin, updates };
      })
    )
    .catch(throwGlobalError);
}

export function installPlugin(data: { key: string }): Promise<void | Response> {
  return post('/api/plugins/install', data).catch(throwGlobalError);
}

export function uninstallPlugin(data: { key: string }): Promise<void | Response> {
  return post('/api/plugins/uninstall', data).catch(throwGlobalError);
}

export function updatePlugin(data: { key: string }): Promise<void | Response> {
  return post('/api/plugins/update', data).catch(throwGlobalError);
}

export function cancelPendingPlugins(): Promise<void | Response> {
  return post('/api/plugins/cancel_all').catch(throwGlobalError);
}
