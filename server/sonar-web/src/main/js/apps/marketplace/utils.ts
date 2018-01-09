/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { memoize, sortBy } from 'lodash';
import { Plugin, PluginAvailable, PluginInstalled, PluginPending } from '../../api/plugins';
import { cleanQuery, parseAsString, RawQuery, serializeString } from '../../helpers/query';
import { Edition } from '../../api/marketplace';

export interface Query {
  filter: string;
  search?: string;
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

export function filterPlugins(plugins: Plugin[], search: string): Plugin[] {
  const s = search.toLowerCase();
  return plugins.filter(plugin => {
    return (
      plugin.name.toLowerCase().includes(s) ||
      plugin.description.toLowerCase().includes(s) ||
      (plugin.category || '').toLowerCase().includes(s)
    );
  });
}

const EDITIONS_ORDER = ['community', 'developer', 'enterprise', 'datacenter'];
export function sortEditions(editions: Edition[]): Edition[] {
  return sortBy(editions, edition => EDITIONS_ORDER.indexOf(edition.key));
}

export const DEFAULT_FILTER = 'all';
export const parseQuery = memoize((urlQuery: RawQuery): Query => ({
  filter: parseAsString(urlQuery['filter']) || DEFAULT_FILTER,
  search: parseAsString(urlQuery['search'])
}));

export const serializeQuery = memoize((query: Query): RawQuery =>
  cleanQuery({
    filter: query.filter === DEFAULT_FILTER ? undefined : serializeString(query.filter),
    search: query.search ? serializeString(query.search) : undefined
  })
);
