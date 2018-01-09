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
import { isNil, omitBy } from 'lodash';
import { isValidDate, parseDate, toNotSoISOString } from './dates';

export interface RawQuery {
  [x: string]: any;
}

function arraysEqual(a: RawQuery, b: RawQuery): boolean {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
}

export function queriesEqual(a: RawQuery, b: RawQuery): boolean {
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);

  if (keysA.length !== keysB.length) {
    return false;
  }

  return keysA.every(
    key =>
      Array.isArray(a[key]) && Array.isArray(b[key])
        ? arraysEqual(a[key], b[key])
        : a[key] === b[key]
  );
}

export function cleanQuery(query: RawQuery): RawQuery {
  return omitBy(query, isNil);
}

export function parseAsBoolean(value: string | undefined, defaultValue: boolean = true): boolean {
  return value === 'false' ? false : value === 'true' ? true : defaultValue;
}

export function parseAsDate(value?: string): Date | undefined {
  if (value) {
    const date = parseDate(value);
    if (isValidDate(date)) {
      return date;
    }
  }
  return undefined;
}

export function parseAsFacetMode(facetMode: string): string {
  return facetMode === 'debt' || facetMode === 'effort' ? 'effort' : 'count';
}

export function parseAsString(value: string | undefined): string {
  return value || '';
}

export function parseAsArray(
  value: string | undefined,
  itemParser: (x: string) => string
): string[] {
  return value ? value.split(',').map(itemParser) : [];
}

export function serializeDate(value?: Date): string | undefined {
  if (value != null && value.toISOString) {
    return toNotSoISOString(value);
  }
  return undefined;
}

export function serializeString(value: string): string | undefined {
  return value || undefined;
}

export function serializeStringArray(value: string[] | undefined[]): string | undefined {
  return value && value.length ? value.join() : undefined;
}
