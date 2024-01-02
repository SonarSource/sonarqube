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
import { isEqual, isNil, omitBy } from 'lodash';
import { RawQuery } from '../types/types';
import { isValidDate, parseDate, toNotSoISOString, toShortNotSoISOString } from './dates';

export function queriesEqual(a: RawQuery, b: RawQuery): boolean {
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);

  if (keysA.length !== keysB.length) {
    return false;
  }

  return keysA.every((key) => isEqual(a[key], b[key]));
}

export function cleanQuery(query: RawQuery): RawQuery {
  return omitBy(query, isNil);
}

export function parseAsBoolean(value: string | undefined, defaultValue = true): boolean {
  if (value === 'false') {
    return false;
  }
  if (value === 'true') {
    return true;
  }
  return defaultValue;
}

export function parseAsOptionalBoolean(value: string | undefined): boolean | undefined {
  if (value === 'true') {
    return true;
  } else if (value === 'false') {
    return false;
  }
  return undefined;
}

export function parseAsDate(value?: string): Date | undefined {
  if (value) {
    // We atttemp to parse date that does not have time.
    // Otherwise date will create a date at midnight UTC
    // and it does not play well when we get the local day.
    let date = parseDate(value + ' 00:00:00');
    if (isValidDate(date)) {
      return date;
    }
    date = parseDate(value);
    if (isValidDate(date)) {
      return date;
    }
  }
  return undefined;
}

export function parseAsString(value: string | undefined): string {
  return value || '';
}

export function parseAsOptionalString(value: string | undefined): string | undefined {
  return value || undefined;
}

export function parseAsArray<T>(value: string | undefined, itemParser: (x: string) => T): T[] {
  return value ? value.split(',').map(itemParser) : [];
}

export function parseAsOptionalArray<T>(
  value: string | undefined,
  itemParser: (x: string) => T
): T[] | undefined {
  return value ? parseAsArray(value, itemParser) : undefined;
}

export function serializeDate(value?: Date, serializer = toNotSoISOString): string | undefined {
  if (value != null) {
    return serializer(value);
  }
  return undefined;
}

export function serializeDateShort(value: Date | undefined): string | undefined {
  return serializeDate(value, toShortNotSoISOString);
}

export function serializeString(value: string | undefined): string | undefined {
  return value || undefined;
}

export function serializeStringArray(value: string[] | undefined[]): string | undefined {
  return value && value.length ? value.join() : undefined;
}

export function serializeOptionalBoolean(value: boolean | undefined): string | undefined {
  if (value === true) {
    return 'true';
  } else if (value === false) {
    return 'false';
  }
  return undefined;
}
