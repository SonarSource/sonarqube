/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import moment from 'moment';

export type RawQuery = { [string]: string };

const arraysEqual = <T>(a: Array<T>, b: Array<T>) => {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
};

export const queriesEqual = <T>(a: T, b: T): boolean => {
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);

  if (keysA.length !== keysB.length) {
    return false;
  }

  return keysA.every(
    key =>
      (Array.isArray(a[key]) && Array.isArray(b[key])
        ? arraysEqual(a[key], b[key])
        : a[key] === b[key])
  );
};

export const cleanQuery = (query: { [string]: ?string }): RawQuery => omitBy(query, isNil);

export const parseAsBoolean = (value: ?string, defaultValue: boolean = true): boolean =>
  (value === 'false' ? false : value === 'true' ? true : defaultValue);

export const parseAsDate = (value: ?string): ?Date => {
  const date = moment(value);
  if (value && date) {
    return date.toDate();
  }
};

export const parseAsFacetMode = (facetMode: string) =>
  (facetMode === 'debt' || facetMode === 'effort' ? 'effort' : 'count');

export const parseAsString = (value: ?string): string => value || '';

export const parseAsArray = <T>(value: ?string, itemParser: string => T): Array<T> =>
  (value ? value.split(',').map(itemParser) : []);

export const serializeDate = (value: ?Date): ?string => {
  if (value != null && value.toISOString) {
    return value.toISOString();
  }
};

export const serializeString = (value: string): ?string => value || undefined;

export const serializeStringArray = (value: ?Array<string>): ?string =>
  (value && value.length ? value.join() : undefined);
