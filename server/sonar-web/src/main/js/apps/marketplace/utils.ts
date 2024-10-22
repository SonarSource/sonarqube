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

import { findLastIndex, memoize } from 'lodash';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { RawQuery } from '~sonar-aligned/types/router';
import { getInstalledPlugins, getUpdatesPlugins } from '../../api/plugins';
import { cleanQuery, parseAsString, serializeString } from '../../helpers/query';
import { isDefined } from '../../helpers/types';
import { InstalledPlugin, Plugin, Update } from '../../types/plugins';

export interface Query {
  filter: string;
  search?: string;
}

const EXCLUDED_PLUGINS = ['license'];
export function filterPlugins(plugins: Plugin[], search?: string): Plugin[] {
  if (!search) {
    return plugins.filter((plugin) => !EXCLUDED_PLUGINS.includes(plugin.key));
  }

  const s = search.toLowerCase();
  return plugins.filter((plugin) => {
    return (
      !EXCLUDED_PLUGINS.includes(plugin.key) &&
      (plugin.name.toLowerCase().includes(s) ||
        (plugin.description || '').toLowerCase().includes(s) ||
        (plugin.category || '').toLowerCase().includes(s))
    );
  });
}

export const DEFAULT_FILTER = 'all';
export const parseQuery = memoize(
  (urlQuery: RawQuery): Query => ({
    filter: parseAsString(urlQuery['filter']) || DEFAULT_FILTER,
    search: parseAsString(urlQuery['search']),
  }),
);

export const serializeQuery = memoize(
  (query: Query): RawQuery =>
    cleanQuery({
      filter: query.filter === DEFAULT_FILTER ? undefined : serializeString(query.filter),
      search: query.search ? serializeString(query.search) : undefined,
    }),
);

function getLastUpdates(updates: undefined | Update[]): Update[] {
  if (!updates) {
    return [];
  }
  const lastUpdate = ['COMPATIBLE', 'REQUIRES_SYSTEM_UPGRADE', 'DEPS_REQUIRE_SYSTEM_UPGRADE'].map(
    (status) => {
      const index = findLastIndex(updates, (update) => update.status === status);
      return index > -1 ? updates[index] : undefined;
    },
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

export function getInstalledPluginsWithUpdates(): Promise<InstalledPlugin[]> {
  return Promise.all([getInstalledPlugins(), getUpdatesPlugins()])
    .then(([installed, updates]) =>
      installed.map((plugin: InstalledPlugin) => {
        const updatePlugin: InstalledPlugin = updates.plugins.find(
          (p: InstalledPlugin) => p.key === plugin.key,
        );
        if (updatePlugin) {
          return {
            ...updatePlugin,
            ...plugin,
            updates: getLastUpdates(updatePlugin.updates).map((update) =>
              addChangelog(update, updatePlugin.updates),
            ),
          };
        }
        return plugin;
      }),
    )
    .catch(throwGlobalError);
}

export function getPluginUpdates(): Promise<InstalledPlugin[]> {
  return Promise.all([getUpdatesPlugins(), getInstalledPlugins()])
    .then(([updates, installed]) =>
      updates.plugins.map((updatePlugin: InstalledPlugin) => {
        const updates = getLastUpdates(updatePlugin.updates).map((update) =>
          addChangelog(update, updatePlugin.updates),
        );
        const plugin = installed.find((p: InstalledPlugin) => p.key === updatePlugin.key);
        if (plugin) {
          return {
            ...plugin,
            ...updatePlugin,
            updates,
          };
        }
        return { ...updatePlugin, updates };
      }),
    )
    .catch(throwGlobalError);
}
