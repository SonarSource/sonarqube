/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { memoize } from 'lodash';
import { cleanQuery, parseAsString, serializeString } from 'sonar-ui-common/helpers/query';
import { Plugin, PluginAvailable, PluginInstalled, PluginPending } from '../../api/plugins';

export interface Query {
  filter: string;
  search?: string;
}

const EXCLUDED_PLUGINS = ['license'];
export function filterPlugins(plugins: Plugin[], search?: string): Plugin[] {
  if (!search) {
    return plugins.filter(plugin => !EXCLUDED_PLUGINS.includes(plugin.key));
  }

  const s = search.toLowerCase();
  return plugins.filter(plugin => {
    return (
      !EXCLUDED_PLUGINS.includes(plugin.key) &&
      (plugin.name.toLowerCase().includes(s) ||
        (plugin.description || '').toLowerCase().includes(s) ||
        (plugin.category || '').toLowerCase().includes(s))
    );
  });
}

export function isPluginAvailable(plugin: Plugin): plugin is PluginAvailable {
  return (plugin as any).release !== undefined;
}

export function isPluginInstalled(plugin: Plugin): plugin is PluginInstalled {
  return isPluginPending(plugin) && (plugin as any).updatedAt !== undefined;
}

export function isPluginPending(plugin: Plugin): plugin is PluginPending {
  return (plugin as any).version !== undefined;
}

export const DEFAULT_FILTER = 'all';
export const parseQuery = memoize(
  (urlQuery: T.RawQuery): Query => ({
    filter: parseAsString(urlQuery['filter']) || DEFAULT_FILTER,
    search: parseAsString(urlQuery['search'])
  })
);

export const serializeQuery = memoize(
  (query: Query): T.RawQuery =>
    cleanQuery({
      filter: query.filter === DEFAULT_FILTER ? undefined : serializeString(query.filter),
      search: query.search ? serializeString(query.search) : undefined
    })
);
