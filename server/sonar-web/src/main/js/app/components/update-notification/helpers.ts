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

import { isEmpty } from 'lodash';
import { sortUpgrades } from '../../../components/upgrade/utils';
import { SystemUpgrade } from '../../../types/system';

const MONTH_BEFOR_PREVIOUS_LTS_NOTIFICATION = 6;

type GroupedSystemUpdate = {
  [x: string]: Record<string, SystemUpgrade[]>;
};

export const isPreLTSUpdate = (parsedVersion: number[], latestLTS: string) => {
  const [currentMajor, currentMinor] = parsedVersion;
  const [ltsMajor, ltsMinor] = latestLTS.split('.').map(Number);
  return currentMajor < ltsMajor || (currentMajor === ltsMajor && currentMinor < ltsMinor);
};

export const isPreviousLTSUpdate = (
  parsedVersion: number[],
  latestLTS: string,
  systemUpgrades: GroupedSystemUpdate,
) => {
  const [ltsMajor, ltsMinor] = latestLTS.split('.').map(Number);
  let ltsOlderThan6Month = false;
  const beforeLts = isPreLTSUpdate(parsedVersion, latestLTS);
  if (beforeLts) {
    const allLTS = sortUpgrades(systemUpgrades[ltsMajor][ltsMinor]);
    const ltsReleaseDate = new Date(allLTS[allLTS.length - 1]?.releaseDate ?? '');
    if (isNaN(ltsReleaseDate.getTime())) {
      // We can not parse the LTS date.
      // It is unlikly that this could happen but consider LTS to be old.
      return true;
    }
    ltsOlderThan6Month =
      ltsReleaseDate.setMonth(ltsReleaseDate.getMonth() + MONTH_BEFOR_PREVIOUS_LTS_NOTIFICATION) -
        Date.now() <
      0;
  }
  return ltsOlderThan6Month && beforeLts;
};

export const isMinorUpdate = (parsedVersion: number[], systemUpgrades: GroupedSystemUpdate) => {
  const [currentMajor, currentMinor] = parsedVersion;
  const allMinor = systemUpgrades[currentMajor];
  return Object.keys(allMinor)
    .map(Number)
    .some((minor) => minor > currentMinor);
};

export const isPatchUpdate = (parsedVersion: number[], systemUpgrades: GroupedSystemUpdate) => {
  const [currentMajor, currentMinor, currentPatch] = parsedVersion;
  const allMinor = systemUpgrades[currentMajor];
  const allPatch = sortUpgrades(allMinor[currentMinor] || []);

  if (!isEmpty(allPatch)) {
    const [, , latestPatch] = allPatch[0].version.split('.').map(Number);
    const effectiveCurrentPatch = isNaN(currentPatch) ? 0 : currentPatch;
    const effectiveLatestPatch = isNaN(latestPatch) ? 0 : latestPatch;
    return effectiveCurrentPatch < effectiveLatestPatch;
  }
  return false;
};
