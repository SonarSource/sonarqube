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
// @flow
import { isNil, omitBy } from 'lodash';
import moment from 'moment';

/*::
export type RawQuery = { [string]: any };
*/

function arraysEqual(a, b) {
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

export function queriesEqual(a /*: Object */, b /*: Object */) /*: boolean */ {
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

export function cleanQuery(query /*: { [string]: ?string } */) /*: RawQuery */ {
  return omitBy(query, isNil);
}

export function parseAsBoolean(
  value /*: ?string */,
  defaultValue /*: boolean */ = true
) /*: boolean */ {
  return value === 'false' ? false : value === 'true' ? true : defaultValue;
}

export function parseAsDate(value /*: ?string */) /*: Date | void */ {
  const date = moment(value);
  if (value && date) {
    return date.toDate();
  }
}

export function parseAsFacetMode(facetMode /*: string */) {
  return facetMode === 'debt' || facetMode === 'effort' ? 'effort' : 'count';
}

export function parseAsString(value /*: ?string */) /*: string */ {
  return value || '';
}

export function parseAsArray(value /*: ?string */, itemParser /*: string => * */) /*: Array<*> */ {
  return value ? value.split(',').map(itemParser) : [];
}

export function serializeDate(value /*: ?Date */) /*: string | void */ {
  if (value != null && value.toISOString) {
    return moment(value).format('YYYY-MM-DDTHH:mm:ssZZ');
  }
}

export function serializeString(value /*: string */) /*: ?string */ {
  return value || undefined;
}

export function serializeStringArray(value /*: ?Array<string> */) /*: ?string */ {
  return value && value.length ? value.join() : undefined;
}
