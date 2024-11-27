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

import { groupBy, isEmpty, mapValues } from 'lodash';
import { Variant } from '~design-system';
import { UpdateUseCase, sortUpgrades } from '../../../components/upgrade/utils';
import { SystemUpgrade } from '../../../types/system';
import { Dict } from '../../../types/types';

type GroupedSystemUpdate = {
  [x: string]: Record<string, SystemUpgrade[]>;
};

export const analyzeUpgrades = ({
  parsedVersion = [],
  upgrades,
}: {
  parsedVersion: number[] | undefined;
  upgrades: SystemUpgrade[];
}) => {
  const systemUpgrades = mapValues(
    groupBy(upgrades, (upgrade: SystemUpgrade) => {
      const [major] = upgrade.version.split('.');
      return major;
    }),
    (upgrades) =>
      groupBy(upgrades, (upgrade: SystemUpgrade) => {
        const [, minor] = upgrade.version.split('.');
        return minor;
      }),
  );

  const latest = [...upgrades].sort(
    (upgrade1, upgrade2) =>
      new Date(upgrade2.releaseDate ?? '').getTime() -
      new Date(upgrade1.releaseDate ?? '').getTime(),
  )[0];

  return {
    isMinorUpdate: isMinorUpdate(parsedVersion, systemUpgrades),
    isPatchUpdate: isLatestUpdatedAPatchUpdate(parsedVersion, systemUpgrades),
    latest,
  };
};

export const isCurrentVersionLTA = (parsedVersion: number[], latestLTS: string) => {
  const [currentMajor, currentMinor] = parsedVersion;
  const [ltsMajor, ltsMinor] = latestLTS.split('.').map(Number);
  return currentMajor === ltsMajor && currentMinor === ltsMinor;
};

export const isMinorUpdate = (parsedVersion: number[], systemUpgrades: GroupedSystemUpdate) => {
  const [currentMajor, currentMinor] = parsedVersion;
  const allMinor = systemUpgrades[currentMajor] ?? {};

  return Object.keys(allMinor)
    .map(Number)
    .some((minor) => minor > currentMinor);
};

export const isLatestUpdatedAPatchUpdate = (
  parsedVersion: number[],
  systemUpgrades: GroupedSystemUpdate,
) => {
  const [currentMajor, currentMinor, currentPatch] = parsedVersion;
  const allMinor = systemUpgrades[currentMajor];
  const allPatch = sortUpgrades(allMinor?.[currentMinor] ?? []);

  if (!isEmpty(allPatch)) {
    const [, , latestPatch] = allPatch[0].version.split('.').map(Number);
    const effectiveCurrentPatch = isNaN(currentPatch) ? 0 : currentPatch;
    const effectiveLatestPatch = isNaN(latestPatch) ? 0 : latestPatch;

    return effectiveCurrentPatch < effectiveLatestPatch;
  }

  return false;
};

export const parseVersion = (version: string) => {
  const VERSION_PARSER = /^(\d+)\.(\d+)(\.(\d+))?/;
  const regExpParsedVersion = VERSION_PARSER.exec(version);

  return regExpParsedVersion
    ?.slice(1)
    .map(Number)
    .map((n) => (isNaN(n) ? 0 : n));
};

export const isVersionAPatchUpdate = (version: string) =>
  ((parseVersion(version) ?? [])[2] ?? 0) !== 0;

export const BANNER_VARIANT: Dict<Variant> = {
  [UpdateUseCase.NewVersion]: 'info',
  [UpdateUseCase.CurrentVersionInactive]: 'error',
  [UpdateUseCase.NewPatch]: 'warning',
};
