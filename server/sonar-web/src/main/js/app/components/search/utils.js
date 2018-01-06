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
// @flow
import { sortBy } from 'lodash';

const ORDER = ['DEV', 'VW', 'SVW', 'APP', 'TRK', 'BRC', 'FIL', 'UTS'];

export function sortQualifiers(qualifiers /*: Array<string> */) {
  return sortBy(qualifiers, qualifier => ORDER.indexOf(qualifier));
}

/*::
export type Component = {
  isFavorite?: boolean,
  isRecentlyBrowsed?: boolean,
  key: string,
  match?: string,
  name: string,
  organization?: string,
  project?: string,
  qualifier: string
};
*/

/*::
export type Results = { [qualifier: string]: Array<Component> };
*/

/*::
export type More = { [string]: number };
*/
