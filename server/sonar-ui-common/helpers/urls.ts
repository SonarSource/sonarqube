/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { isNil, omitBy } from 'lodash';
import { stringify } from 'querystring';
import { getUrlContext, IS_SSR } from './init';

interface Query {
  [x: string]: string | undefined;
}

export interface Location {
  pathname: string;
  query?: Query;
}

export function getBaseUrl(): string {
  return getUrlContext();
}

export function getHostUrl(): string {
  if (IS_SSR) {
    throw new Error('No host url available on server side.');
  }
  return window.location.origin + getBaseUrl();
}

export function getPathUrlAsString(path: Location, internal = true): string {
  return `${internal ? getBaseUrl() : getHostUrl()}${path.pathname}?${stringify(
    omitBy(path.query, isNil)
  )}`;
}

export function getReturnUrl(location: { hash?: string; query?: { return_to?: string } }) {
  const returnTo = location.query && location.query['return_to'];
  if (isRelativeUrl(returnTo)) {
    return returnTo + (location.hash ? location.hash : '');
  }
  return getBaseUrl() + '/';
}

export function isRelativeUrl(url?: string): boolean {
  const regex = new RegExp(/^\/[^/\\]/);
  return Boolean(url && regex.test(url));
}
