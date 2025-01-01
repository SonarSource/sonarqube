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

import axios from 'axios';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { post, postJSON, requestTryAndRepeatUntil } from '../helpers/request';
import {
  EmailConfiguration,
  MigrationStatus,
  MigrationsStatusResponse,
  SystemUpgrade,
} from '../types/system';
import { Paging, SysInfoCluster, SysInfoStandalone, SysStatus } from '../types/types';

const MIGRATIONS_STATUS_ENDPOINT = '/api/v2/system/migrations-status';
const EMAIL_NOTIFICATION_PATH = '/api/v2/system/email-configurations';

export function setLogLevel(level: string): Promise<void | Response> {
  return post('/api/system/change_log_level', { level }).catch(throwGlobalError);
}

export function getSystemInfo(): Promise<SysInfoCluster | SysInfoStandalone> {
  return getJSON('/api/system/info').catch(throwGlobalError);
}

export function getSystemStatus(): Promise<{ id: string; status: SysStatus; version: string }> {
  return getJSON('/api/system/status');
}

export function getSystemUpgrades(): Promise<{
  installedVersionActive?: boolean;
  latestLTA?: string;
  updateCenterRefresh?: string;
  upgrades: SystemUpgrade[];
}> {
  return getJSON('/api/system/upgrades');
}

export function getMigrationsStatus() {
  return axios.get<MigrationsStatusResponse>(MIGRATIONS_STATUS_ENDPOINT);
}

export function migrateDatabase(): Promise<{
  message?: string;
  startedAt?: string;
  state: MigrationStatus;
}> {
  return postJSON('/api/system/migrate_db');
}

export function restart(): Promise<void | Response> {
  return post('/api/system/restart').catch(throwGlobalError);
}

export function waitSystemUPStatus(): Promise<{
  id: string;
  status: SysStatus;
  version: string;
}> {
  return requestTryAndRepeatUntil(
    getSystemStatus,
    { max: -1, slowThreshold: -15 },
    ({ status }) => status === 'UP',
  );
}

export function getEmailConfigurations(): Promise<{
  emailConfigurations: EmailConfiguration[];
  page: Paging;
}> {
  return axios.get(EMAIL_NOTIFICATION_PATH);
}

export function postEmailConfiguration(data: EmailConfiguration): Promise<EmailConfiguration> {
  return axios.post<EmailConfiguration, EmailConfiguration>(EMAIL_NOTIFICATION_PATH, data);
}

export function patchEmailConfiguration(
  id: string,
  emailConfiguration: EmailConfiguration,
): Promise<EmailConfiguration> {
  return axios.patch<EmailConfiguration, EmailConfiguration>(
    `${EMAIL_NOTIFICATION_PATH}/${id}`,
    emailConfiguration,
  );
}
