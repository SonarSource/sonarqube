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
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON, post } from '../helpers/request';
import {
  AvailablePlugin,
  InstalledPlugin,
  PendingPluginResult,
  PluginType,
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

export function getInstalledPlugins(type = PluginType.External): Promise<InstalledPlugin[]> {
  return getJSON('/api/plugins/installed', { f: 'category', type }).then(
    ({ plugins }) => plugins,
    throwGlobalError,
  );
}

export function getUpdatesPlugins() {
  return getJSON('/api/plugins/updates');
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
