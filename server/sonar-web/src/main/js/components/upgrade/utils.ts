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

import { groupBy, sortBy } from 'lodash';
import { SystemUpgrade } from '../../types/system';

export enum UpdateUseCase {
  NewVersion = 'new_version',
  CurrentVersionInactive = 'current_version_inactive',
  NewPatch = 'new_patch',
}

export const SYSTEM_VERSION_REGEXP = /^(\d+)\.(\d+)(\.(\d+))?/;

export function sortUpgrades(upgrades: SystemUpgrade[]): SystemUpgrade[] {
  return sortBy(upgrades, [
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[0]),
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[1] || 0),
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[2] || 0),
  ]);
}

export function groupUpgrades(upgrades: SystemUpgrade[]): SystemUpgrade[][] {
  const groupedVersions = groupBy(upgrades, (upgrade) => upgrade.version.split('.')[0]);
  const sortedMajor = sortBy(Object.keys(groupedVersions), (key) => -Number(key));
  return sortedMajor.map((key) => groupedVersions[key]);
}
