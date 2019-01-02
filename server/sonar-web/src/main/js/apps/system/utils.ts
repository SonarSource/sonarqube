/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { each, groupBy, memoize, omit, omitBy, pickBy, sortBy } from 'lodash';
import {
  cleanQuery,
  parseAsArray,
  parseAsString,
  RawQuery,
  serializeStringArray
} from '../../helpers/query';
import {
  ClusterSysInfo,
  HealthType,
  NodeInfo,
  SysInfo,
  SysInfoSection,
  SysValueObject,
  SystemUpgrade
} from '../../api/system';
import { formatMeasure } from '../../helpers/measures';

export interface Query {
  expandedCards: string[];
}

export const LOGS_LEVELS = ['INFO', 'DEBUG', 'TRACE'];
export const HA_FIELD = 'High Availability';
export const HEALTH_FIELD = 'Health';
export const HEALTHCAUSES_FIELD = 'Health Causes';
export const PLUGINS_FIELD = 'Plugins';
export const SETTINGS_FIELD = 'Settings';

export function ignoreInfoFields(sysInfoObject: SysValueObject): SysValueObject {
  return omit(sysInfoObject, [
    HEALTH_FIELD,
    HEALTHCAUSES_FIELD,
    'Name',
    PLUGINS_FIELD,
    SETTINGS_FIELD
  ]) as SysValueObject;
}

export function getHealth(sysInfoObject: SysValueObject): HealthType {
  return sysInfoObject[HEALTH_FIELD] as HealthType;
}

export function getHealthCauses(sysInfoObject: SysValueObject): string[] {
  return sysInfoObject[HEALTHCAUSES_FIELD] as string[];
}

export function getLogsLevel(sysInfoObject?: SysValueObject): string {
  if (!sysInfoObject) {
    return LOGS_LEVELS[0];
  }
  if (sysInfoObject['Web Logging'] || sysInfoObject['Compute Engine Logging']) {
    return sortBy(
      [
        getLogsLevel((sysInfoObject as NodeInfo)['Web Logging']),
        getLogsLevel((sysInfoObject as NodeInfo)['Compute Engine Logging'])
      ],
      logLevel => LOGS_LEVELS.indexOf(logLevel)
    )[1];
  }
  if (sysInfoObject['System']) {
    return getLogsLevel((sysInfoObject as SysInfo)['System']);
  }
  return (sysInfoObject['Logs Level'] || LOGS_LEVELS[0]) as string;
}

export function getAppNodes(sysInfoData: ClusterSysInfo): NodeInfo[] {
  return sysInfoData['Application Nodes'];
}

export function getSearchNodes(sysInfoData: ClusterSysInfo): NodeInfo[] {
  return sysInfoData['Search Nodes'];
}

export function isCluster(sysInfoData?: SysInfo): boolean {
  return (
    sysInfoData !== undefined && sysInfoData['System'] && sysInfoData['System'][HA_FIELD] === true
  );
}

export function getServerId(sysInfoData?: SysInfo): string | undefined {
  return sysInfoData && sysInfoData['System']['Server ID'];
}

export function getSystemLogsLevel(sysInfoData?: SysInfo): string {
  const defaultLevel = LOGS_LEVELS[0];
  if (!sysInfoData) {
    return defaultLevel;
  }
  if (isCluster(sysInfoData)) {
    const logLevels = sortBy(
      getAppNodes(sysInfoData as ClusterSysInfo).map(getLogsLevel),
      logLevel => LOGS_LEVELS.indexOf(logLevel)
    );
    return logLevels.length > 0 ? logLevels[logLevels.length - 1] : defaultLevel;
  } else {
    return getLogsLevel(sysInfoData);
  }
}

export function getNodeName(nodeInfo: NodeInfo): string {
  return nodeInfo['Name'];
}

function getSystemData(sysInfoData: SysInfo): SysValueObject {
  const statData: SysValueObject = {};
  const statistics = sysInfoData['Statistics'] as SysValueObject;
  if (statistics) {
    statData['Lines of Code'] = formatMeasure(statistics['ncloc'] as number, 'INT');
  }
  return { ...sysInfoData['System'], ...statData };
}

export function getClusterMainCardSection(sysInfoData: ClusterSysInfo): SysValueObject {
  return {
    ...getSystemData(sysInfoData),
    ...(omit(sysInfoData, [
      'Application Nodes',
      PLUGINS_FIELD,
      'Search Nodes',
      SETTINGS_FIELD,
      'Statistics',
      'System'
    ]) as SysValueObject)
  };
}

export function getStandaloneMainSections(sysInfoData: SysInfo): SysValueObject {
  return {
    ...getSystemData(sysInfoData),
    ...(omitBy(
      sysInfoData,
      (value, key) =>
        value == null ||
        [PLUGINS_FIELD, SETTINGS_FIELD, 'Statistics', 'System'].includes(key) ||
        key.startsWith('Compute Engine') ||
        key.startsWith('Search') ||
        key.startsWith('Web')
    ) as SysValueObject)
  };
}

export function getStandaloneSecondarySections(sysInfoData: SysInfo): SysInfoSection {
  return {
    Web: pickBy(sysInfoData, (_, key) => key.startsWith('Web')) as SysValueObject,
    'Compute Engine': pickBy(sysInfoData, (_, key) =>
      key.startsWith('Compute Engine')
    ) as SysValueObject,
    'Search Engine': pickBy(sysInfoData, (_, key) => key.startsWith('Search')) as SysValueObject
  };
}

export function getFileNameSuffix(suffix?: string) {
  const now = new Date();
  return (
    `${suffix ? suffix + '-' : ''}` +
    `${now.getFullYear()}-${now.getMonth() + 1}-` +
    `${now.getDate()}-${now.getHours()}-${now.getMinutes()}`
  );
}

export function groupSections(sysInfoData: SysValueObject) {
  const mainSection: SysValueObject = {};
  const sections: SysInfoSection = {};
  each(sysInfoData, (item, key) => {
    if (typeof item !== 'object' || item instanceof Array) {
      mainSection[key] = item;
    } else {
      sections[key] = item;
    }
  });
  return { mainSection, sections };
}

export const parseQuery = memoize(
  (urlQuery: RawQuery): Query => ({
    expandedCards: parseAsArray(urlQuery.expand, parseAsString)
  })
);

export const serializeQuery = memoize(
  (query: Query): RawQuery =>
    cleanQuery({
      expand: serializeStringArray(query.expandedCards)
    })
);

export function sortUpgrades(upgrades: SystemUpgrade[]): SystemUpgrade[] {
  return sortBy(upgrades, [
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[0]),
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[1] || 0),
    (upgrade: SystemUpgrade) => -Number(upgrade.version.split('.')[2] || 0)
  ]);
}

export function groupUpgrades(upgrades: SystemUpgrade[]): SystemUpgrade[][] {
  const groupedVersions = groupBy(upgrades, upgrade => upgrade.version.split('.')[0]);
  const sortedMajor = sortBy(Object.keys(groupedVersions), key => -Number(key));
  return sortedMajor.map(key => groupedVersions[key]);
}
