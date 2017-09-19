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
import { each, memoize, omit, omitBy, pickBy, sortBy } from 'lodash';
import {
  cleanQuery,
  parseAsArray,
  parseAsString,
  RawQuery,
  serializeStringArray
} from '../../helpers/query';
import { HealthType, NodeInfo, SysInfo, SysInfoSection, SysValueObject } from '../../api/system';

export interface Query {
  expandedCards: string[];
}

export interface ClusterSysInfo extends SysInfo {
  'Application Nodes': NodeInfo[];
  'Search Nodes': NodeInfo[];
}

export const LOGS_LEVELS = ['INFO', 'DEBUG', 'TRACE'];
export const HA_FIELD = 'High Availability';
export const HEALTH_FIELD = 'Health';
export const HEALTHCAUSES_FIELD = 'Health Causes';

export function ignoreInfoFields(sysInfoObject: SysValueObject): SysValueObject {
  return omit(sysInfoObject, [HA_FIELD, HEALTH_FIELD, HEALTHCAUSES_FIELD]);
}

export function getHealth(sysInfoObject: SysValueObject): HealthType {
  if (sysInfoObject['System']) {
    return (sysInfoObject as SysInfo)['System'][HEALTH_FIELD];
  }
  return sysInfoObject[HEALTH_FIELD] as HealthType;
}

export function getHealthCauses(sysInfoObject: SysValueObject): string[] {
  if (sysInfoObject['System']) {
    return (sysInfoObject as SysInfo)['System'][HEALTHCAUSES_FIELD];
  }
  return sysInfoObject[HEALTHCAUSES_FIELD] as string[];
}

export function getLogsLevel(sysInfoObject: SysValueObject): string {
  if (sysInfoObject['System']) {
    return (sysInfoObject as SysInfo)['System']['Logs Level'];
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
    sysInfoData != undefined && sysInfoData['System'] && sysInfoData['System'][HA_FIELD] === true
  );
}

export function getSystemLogsLevel(sysInfoData?: SysInfo): string {
  const defaultLevel = LOGS_LEVELS[0];
  if (!sysInfoData) {
    return defaultLevel;
  }
  if (isCluster(sysInfoData)) {
    const nodes = sortBy(getAppNodes(sysInfoData as ClusterSysInfo), node =>
      LOGS_LEVELS.indexOf(getLogsLevel(node))
    );
    return nodes.length > 0 ? getLogsLevel(nodes[nodes.length - 1]) : defaultLevel;
  } else {
    return getLogsLevel(sysInfoData);
  }
}

export function getNodeName(nodeInfo: NodeInfo): string {
  return nodeInfo['Name'];
}

export function getClusterMainCardSection(sysInfoData: ClusterSysInfo): SysValueObject {
  return {
    ...sysInfoData['System'],
    ...omit(sysInfoData, [
      'Application Nodes',
      'Plugins',
      'Search Nodes',
      'Settings',
      'Statistics',
      'System'
    ])
  };
}

export function getStandaloneMainSections(sysInfoData: SysInfo): SysValueObject {
  return {
    ...sysInfoData['System'],
    ...omitBy(
      sysInfoData,
      (value, key) =>
        value == null ||
        ['Plugins', 'Settings', 'Statistics', 'System'].includes(key) ||
        key.startsWith('Compute Engine') ||
        key.startsWith('Search') ||
        key.startsWith('Web')
    )
  };
}

export function getStandaloneSecondarySections(sysInfoData: SysInfo): SysInfoSection {
  return {
    Web: pickBy(sysInfoData, (_, key) => key.startsWith('Web')),
    'Compute Engine': pickBy(sysInfoData, (_, key) => key.startsWith('Compute Engine')),
    Search: pickBy(sysInfoData, (_, key) => key.startsWith('Search'))
  };
}

export function groupSections(sysInfoData: SysValueObject) {
  let mainSection: SysValueObject = {};
  let sections: SysInfoSection = {};
  each(sysInfoData, (item, key) => {
    if (typeof item !== 'object' || item instanceof Array) {
      mainSection[key] = item;
    } else {
      sections[key] = item;
    }
  });
  return { mainSection, sections };
}

export const parseQuery = memoize((urlQuery: RawQuery): Query => ({
  expandedCards: parseAsArray(urlQuery.expand, parseAsString)
}));

export const serializeQuery = memoize((query: Query): RawQuery =>
  cleanQuery({
    expand: serializeStringArray(query.expandedCards)
  })
);
