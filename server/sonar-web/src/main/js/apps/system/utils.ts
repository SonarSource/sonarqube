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
import { each, memoize, omit, omitBy, pickBy, sortBy } from 'lodash';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { RawQuery } from '~sonar-aligned/types/router';
import { cleanQuery, parseAsArray, parseAsString, serializeStringArray } from '../../helpers/query';
import {
  SysInfoAppNode,
  SysInfoBase,
  SysInfoCluster,
  SysInfoLogging,
  SysInfoSearchNode,
  SysInfoSection,
  SysInfoStandalone,
  SysInfoValueObject,
} from '../../types/types';

export interface Query {
  expandedCards: string[];
}

export enum LogsLevels {
  INFO = 'INFO',
  DEBUG = 'DEBUG',
  TRACE = 'TRACE',
}

export const LOGS_LEVELS = Object.values(LogsLevels);
const DEFAULT_LOG_LEVEL = LogsLevels.INFO;

export const APP_NODES_FIELD = 'Application Nodes';
export const ALMS_FIELD = 'ALMs';
export const BUNDLED_FIELD = 'Bundled';
export const CE_FIELD_PREFIX = 'Compute Engine';
export const CE_LOGGING_FIELD = 'Compute Engine Logging';
export const HA_FIELD = 'High Availability';
export const HEALTH_CAUSES_FIELD = 'Health Causes';
export const HEALTH_FIELD = 'Health';
export const LOGS_LEVEL_FIELD = 'Logs Level';
export const NAME_FIELD = 'Name';
export const NCLOC_FIELD = 'ncloc';
export const PLUGINS_FIELD = 'Plugins';
export const SEARCH_NODES_FIELD = 'Search Nodes';
export const SEARCH_PREFIX = 'Search';
export const SERVER_ID_FIELD = 'Server ID';
export const SETTINGS_FIELD = 'Settings';
export const STATE_FIELD = 'State';
export const STATS_FIELD = 'Statistics';
export const SYSTEM_FIELD = 'System';
export const VERSION_FIELD = 'Version';
export const WEB_LOGGING_FIELD = 'Web Logging';
export const WEB_PREFIX = 'Web';

export function ignoreInfoFields(sysInfoObject: SysInfoValueObject) {
  return omit(sysInfoObject, [
    ALMS_FIELD,
    BUNDLED_FIELD,
    HEALTH_FIELD,
    HEALTH_CAUSES_FIELD,
    NAME_FIELD,
    PLUGINS_FIELD,
    SETTINGS_FIELD,
    SERVER_ID_FIELD,
    VERSION_FIELD,
  ]);
}

export function getHealth(sysInfoObject: SysInfoBase) {
  return sysInfoObject[HEALTH_FIELD];
}

export function getHealthCauses(sysInfoObject: SysInfoBase) {
  return sysInfoObject[HEALTH_CAUSES_FIELD];
}

export function getLogsLevel(sysInfoObject?: SysInfoValueObject): LogsLevels {
  if (sysInfoObject !== undefined) {
    if (isLogInfoBlock(sysInfoObject)) {
      return sysInfoObject[LOGS_LEVEL_FIELD] as LogsLevels;
    } else if (hasLoggingInfo(sysInfoObject)) {
      return sortBy(
        [
          getLogsLevel(sysInfoObject[WEB_LOGGING_FIELD]),
          getLogsLevel(sysInfoObject[CE_LOGGING_FIELD]),
        ],
        (logLevel: LogsLevels) => LOGS_LEVELS.indexOf(logLevel),
      )[1];
    }
  }
  return DEFAULT_LOG_LEVEL;
}

export function getAppNodes(sysInfoData: SysInfoCluster): SysInfoAppNode[] {
  return sysInfoData[APP_NODES_FIELD];
}

export function getSearchNodes(sysInfoData: SysInfoCluster): SysInfoSearchNode[] {
  return sysInfoData[SEARCH_NODES_FIELD];
}

export function isCluster(
  sysInfoData: SysInfoCluster | SysInfoStandalone,
): sysInfoData is SysInfoCluster {
  return sysInfoData[SYSTEM_FIELD] && sysInfoData[SYSTEM_FIELD][HA_FIELD] === true;
}

export function isLogInfoBlock(sysInfoObject: SysInfoValueObject): sysInfoObject is SysInfoLogging {
  return sysInfoObject[LOGS_LEVEL_FIELD] !== undefined;
}

export function hasLoggingInfo(
  sysInfoObject: SysInfoValueObject,
): sysInfoObject is SysInfoStandalone | SysInfoAppNode {
  return Boolean(sysInfoObject[WEB_LOGGING_FIELD] || sysInfoObject[CE_LOGGING_FIELD]);
}

export function getServerId(sysInfoData: SysInfoCluster | SysInfoStandalone): string {
  return sysInfoData && sysInfoData[SYSTEM_FIELD][SERVER_ID_FIELD];
}

export function getVersion(sysInfoData: SysInfoStandalone): string | undefined {
  return sysInfoData && sysInfoData[SYSTEM_FIELD][VERSION_FIELD];
}

export function getClusterVersion(sysInfoData: SysInfoCluster): string | undefined {
  const appNodes = getAppNodes(sysInfoData);
  return appNodes.length > 0 ? appNodes[0][SYSTEM_FIELD][VERSION_FIELD] : undefined;
}

export function getSystemLogsLevel(sysInfoData: SysInfoCluster | SysInfoStandalone): string {
  if (isCluster(sysInfoData)) {
    const logLevels = sortBy(getAppNodes(sysInfoData).map(getLogsLevel), (logLevel: LogsLevels) =>
      LOGS_LEVELS.indexOf(logLevel),
    );
    return logLevels.length > 0 ? logLevels[logLevels.length - 1] : DEFAULT_LOG_LEVEL;
  }

  return getLogsLevel(sysInfoData);
}

export function getNodeName(nodeInfo: SysInfoAppNode | SysInfoSearchNode): string {
  return nodeInfo[NAME_FIELD];
}

function getSystemData(sysInfoData: SysInfoBase): SysInfoValueObject {
  const statData: SysInfoValueObject = {};
  const statistics = sysInfoData[STATS_FIELD] as SysInfoValueObject; // TODO
  if (statistics) {
    statData['Lines of Code'] = formatMeasure(statistics[NCLOC_FIELD] as number, 'INT');
  }
  return { ...sysInfoData[SYSTEM_FIELD], ...statData };
}

export function getClusterMainCardSection(sysInfoData: SysInfoCluster): SysInfoValueObject {
  return {
    ...getSystemData(sysInfoData),
    ...omit(sysInfoData, [
      APP_NODES_FIELD,
      PLUGINS_FIELD,
      SEARCH_NODES_FIELD,
      SETTINGS_FIELD,
      STATS_FIELD,
      SYSTEM_FIELD,
    ]),
  };
}

export function getStandaloneMainSections(sysInfoData: SysInfoBase): SysInfoValueObject {
  return {
    ...getSystemData(sysInfoData),
    ...(omitBy(
      sysInfoData,
      (value, key) =>
        value == null ||
        [PLUGINS_FIELD, SETTINGS_FIELD, STATS_FIELD, SYSTEM_FIELD].includes(key) ||
        key.startsWith(CE_FIELD_PREFIX) ||
        key.startsWith(SEARCH_PREFIX) ||
        key.startsWith(WEB_PREFIX),
    ) as SysInfoValueObject),
  };
}

export function getStandaloneSecondarySections(sysInfoData: SysInfoBase): SysInfoSection {
  return {
    Web: pickBy(sysInfoData, (_, key) => key.startsWith(WEB_PREFIX)) as SysInfoValueObject,
    'Compute Engine': pickBy(sysInfoData, (_, key) =>
      key.startsWith(CE_FIELD_PREFIX),
    ) as SysInfoValueObject,
    'Search Engine': pickBy(sysInfoData, (_, key) =>
      key.startsWith(SEARCH_PREFIX),
    ) as SysInfoValueObject,
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

export function groupSections(sysInfoData: SysInfoValueObject) {
  const mainSection: SysInfoValueObject = {};
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
    expandedCards: parseAsArray(urlQuery.expand, parseAsString),
  }),
);

export const serializeQuery = memoize(
  (query: Query): RawQuery =>
    cleanQuery({
      expand: serializeStringArray(query.expandedCards),
    }),
);
