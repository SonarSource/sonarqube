/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import {
  cleanQuery,
  RawQuery,
  serializeString,
  parseAsOptionalBoolean,
  parseAsString
} from '../../helpers/query';

export interface Query {
  search: string;
  deprecated: boolean;
  internal: boolean;
}

export function actionsFilter(query: Query, domain: T.WebApi.Domain, action: T.WebApi.Action) {
  const lowSearchQuery = query.search.toLowerCase();
  return (
    (query.internal || !action.internal) &&
    (query.deprecated || !action.deprecatedSince) &&
    (getActionKey(domain.path, action.key).includes(lowSearchQuery) ||
      (action.description || '').toLowerCase().includes(lowSearchQuery))
  );
}

export function getActionKey(domainPath: string, actionKey: string) {
  return domainPath + '/' + actionKey;
}

export const isDomainPathActive = (path: string, splat: string) => {
  const pathTokens = path.split('/');
  const splatTokens = splat.split('/');

  if (pathTokens.length > splatTokens.length) {
    return false;
  }

  for (let i = 0; i < pathTokens.length; i++) {
    if (pathTokens[i] !== splatTokens[i]) {
      return false;
    }
  }

  return true;
};

export const parseQuery = memoize(
  (urlQuery: RawQuery): Query => ({
    search: parseAsString(urlQuery['query']),
    deprecated: parseAsOptionalBoolean(urlQuery['deprecated']) || false,
    internal: parseAsOptionalBoolean(urlQuery['internal']) || false
  })
);

export const serializeQuery = memoize(
  (query: Partial<Query>): RawQuery =>
    cleanQuery({
      query: query.search ? serializeString(query.search) : undefined,
      deprecated: query.deprecated || undefined,
      internal: query.internal || undefined
    })
);

export function parseVersion(version: string) {
  const match = /(\d+)\.(\d+)/.exec(version);
  if (match) {
    return { major: Number(match[1]), minor: Number(match[2]) };
  } else {
    return undefined;
  }
}
