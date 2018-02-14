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
import { getJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export type SysValue = boolean | string | number | HealthType | SysValueObject | SysValueArray;
export interface SysValueObject {
  [key: string]: SysValue;
}
export interface SysValueArray extends Array<SysValue> {}

export interface SysInfoSection {
  [sectionName: string]: SysValueObject;
}

export enum HealthType {
  RED = 'RED',
  YELLOW = 'YELLOW',
  GREEN = 'GREEN'
}

export interface NodeInfo extends SysValueObject {
  'Compute Engine Logging': { 'Logs Level': string };
  Health: HealthType;
  'Health Causes': string[];
  Name: string;
  'Web Logging': { 'Logs Level': string };
}

export interface SysInfo extends SysValueObject {
  Health: HealthType;
  'Health Causes': string[];
  System: {
    'High Availability': boolean;
    'Logs Level': string;
    'Server ID': string;
  };
}

export interface ClusterSysInfo extends SysInfo {
  'Application Nodes': NodeInfo[];
  'Search Nodes': NodeInfo[];
}

export interface SystemUpgrade {
  version: string;
  description: string;
  releaseDate: string;
  changeLogUrl: string;
  downloadUrl: string;
  plugins: any;
}

export function setLogLevel(level: string): Promise<void | Response> {
  return post('/api/system/change_log_level', { level }).catch(throwGlobalError);
}

export function getSystemInfo(): Promise<SysInfo> {
  return getJSON('/api/system/info').catch(throwGlobalError);
}

export function getSystemStatus(): Promise<{ id: string; version: string; status: string }> {
  return getJSON('/api/system/status');
}

export function getSystemUpgrades(): Promise<{
  upgrades: SystemUpgrade[];
  updateCenterRefresh: string;
}> {
  return getJSON('/api/system/upgrades');
}

export function getMigrationStatus(): Promise<{
  message?: string;
  startedAt?: string;
  state: string;
}> {
  return getJSON('/api/system/db_migration_status');
}

export function migrateDatabase(): Promise<{
  message?: string;
  startedAt?: string;
  state: string;
}> {
  return postJSON('/api/system/migrate_db');
}

export function restart(): Promise<void | Response> {
  return post('/api/system/restart').catch(throwGlobalError);
}

const POLLING_INTERVAL = 2000;

function pollStatus(cb: Function): void {
  setTimeout(() => {
    getSystemStatus()
      .then(r => {
        if (r.status === 'UP') {
          cb();
        } else {
          pollStatus(cb);
        }
      })
      .catch(() => pollStatus(cb));
  }, POLLING_INTERVAL);
}

function promiseStatus(): Promise<any> {
  return new Promise(resolve => pollStatus(resolve));
}

export function restartAndWait(): Promise<any> {
  return restart().then(promiseStatus);
}
