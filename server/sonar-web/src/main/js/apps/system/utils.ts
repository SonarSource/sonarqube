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
import { each, omit, memoize, sortBy } from 'lodash';
import {
  cleanQuery,
  parseAsArray,
  parseAsString,
  RawQuery,
  serializeStringArray
} from '../../helpers/query';
import {
  HealthCause,
  HealthType,
  NodeInfo,
  SysInfo,
  SysInfoSection,
  SysValueObject
} from '../../api/system';

export interface Query {
  expandedCards: string[];
}

export interface ClusterSysInfo extends SysInfo {
  'Application Nodes': NodeInfo[];
  'Search Nodes': NodeInfo[];
}

export interface StandaloneSysInfo extends SysInfo {
  'Logs Level': string;
}

export const LOGS_LEVELS = ['INFO', 'DEBUG', 'TRACE'];
export const HEALTH_FIELD = 'Health';
export const HEALTHCAUSES_FIELD = 'Health Causes';

export function ignoreInfoFields(sysInfoObject: SysValueObject): SysValueObject {
  return omit(sysInfoObject, ['Cluster', HEALTH_FIELD, HEALTHCAUSES_FIELD]);
}

export function getHealth(sysInfoObject: SysValueObject): HealthType {
  return sysInfoObject[HEALTH_FIELD] as HealthType;
}

export function getHealthCauses(sysInfoObject: SysValueObject): HealthCause[] {
  return sysInfoObject[HEALTHCAUSES_FIELD] as HealthCause[];
}

export function getLogsLevel(sysInfoData?: SysInfo): string {
  const defaultLevel = LOGS_LEVELS[0];
  if (!sysInfoData) {
    return defaultLevel;
  }
  if (isCluster(sysInfoData)) {
    const nodes = sortBy(getAppNodes(sysInfoData as ClusterSysInfo), node =>
      LOGS_LEVELS.indexOf(node['Logs Level'])
    );
    return nodes[nodes.length - 1]['Logs Level'] || defaultLevel;
  } else {
    return (sysInfoData as StandaloneSysInfo)['Logs Level'] || defaultLevel;
  }
}

export function getNodeName(nodeInfo: NodeInfo): string {
  return nodeInfo['Name'];
}

export function getClusterMainCardSection(sysInfoData: ClusterSysInfo): SysValueObject {
  return omit(sysInfoData, ['Application Nodes', 'Search Nodes', 'Settings', 'Statistics']);
}

export function getStandaloneMainSections(sysInfoData: StandaloneSysInfo): SysValueObject {
  return omit(sysInfoData, [
    'Settings',
    'Statistics',
    'Compute Engine',
    'Compute Engine JVM',
    'Compute Engine JVM Properties',
    'Elasticsearch',
    'Search JVM',
    'Search JVM Properties',
    'Web Database Connectivity',
    'Web JVM',
    'Web JVM Properties'
  ]);
}

export function getStandaloneSecondarySections(sysInfoData: StandaloneSysInfo): SysInfoSection {
  return {
    Web: {
      'Web Database Connectivity': sysInfoData['Web Database Connectivity'],
      'Web JVM': sysInfoData['Web JVM'],
      'Web JVM Properties': sysInfoData['Web JVM Properties']
    },
    'Compute Engine': {
      ...sysInfoData['Compute Engine'] as SysValueObject,
      'Compute Engine JVM': sysInfoData['Compute Engine JVM'],
      'Compute Engine JVM Properties': sysInfoData['Compute Engine JVM Properties']
    },
    Search: {
      Elasticsearch: sysInfoData['Elasticsearch'] as SysValueObject,
      'Search JVM': sysInfoData['Search JVM'],
      'Search JVM Properties': sysInfoData['Search JVM Properties']
    }
  };
}

export function getAppNodes(sysInfoData: ClusterSysInfo): NodeInfo[] {
  return sysInfoData['Application Nodes'];
}

export function getSearchNodes(sysInfoData: ClusterSysInfo): NodeInfo[] {
  return sysInfoData['Search Nodes'];
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

export function isCluster(sysInfoData?: SysInfo): boolean {
  return sysInfoData != undefined && sysInfoData['Cluster'] === true;
}

export const parseQuery = memoize((urlQuery: RawQuery): Query => {
  return {
    expandedCards: parseAsArray(urlQuery.expand, parseAsString)
  };
});

export const serializeQuery = memoize((query: Query): RawQuery => {
  return cleanQuery({
    expand: serializeStringArray(query.expandedCards)
  });
});
