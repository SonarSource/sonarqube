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
import { sortBy } from 'lodash';
import { Edition, EditionsPerVersion } from '../../api/marketplace';

export function getEditionsForLastVersion(editions: EditionsPerVersion): Edition[] {
  const sortedVersion = sortBy(Object.keys(editions), [
    (version: string) => -Number(version.split('.')[0]),
    (version: string) => -Number(version.split('.')[1] || 0),
    (version: string) => -Number(version.split('.')[2] || 0)
  ]);
  return editions[sortedVersion[0]];
}

export function getEditionsForVersion(
  editions: EditionsPerVersion,
  version: string
): Edition[] | undefined {
  const minorVersion = version.match(/\d+\.\d+.\d+/);
  if (minorVersion) {
    if (editions[minorVersion[0]]) {
      return editions[minorVersion[0]];
    }
  }
  const majorVersion = version.match(/\d+\.\d+/);
  if (majorVersion) {
    if (editions[majorVersion[0]]) {
      return editions[majorVersion[0]];
    }
  }
  return undefined;
}
