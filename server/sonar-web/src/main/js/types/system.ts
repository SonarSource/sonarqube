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
export interface SystemUpgradeDownloadUrls {
  downloadDatacenterUrl?: string;
  downloadDeveloperUrl?: string;
  downloadEnterpriseUrl?: string;
  downloadUrl: string;
}

export interface SystemUpgrade extends SystemUpgradeDownloadUrls {
  changeLogUrl?: string;
  description?: string;
  releaseDate?: string;
  version: string;
}

export enum InstanceType {
  SonarQube = 'SonarQube',
  SonarCloud = 'SonarCloud',
}

export enum MigrationStatus {
  noMigration = 'NO_MIGRATION',
  notSupported = 'NOT_SUPPORTED',
  required = 'MIGRATION_REQUIRED',
  running = 'MIGRATION_RUNNING',
  succeeded = 'MIGRATION_SUCCEEDED',
  failed = 'MIGRATION_FAILED',
}

export interface MigrationsStatusResponse {
  completedSteps?: number;
  expectedFinishTimestamp?: string;
  message?: string;
  startedAt?: string;
  status: MigrationStatus;
  totalSteps?: number;
}

export enum AuthMethod {
  Basic = 'BASIC',
  OAuth = 'OAUTH',
}

export type EmailConfiguration = EmailConfigurationAuth & EmailConfigurationCommon;
export type EmailConfigurationAuth = EmailNotificationBasicAuth | EmailNotificationOAuth;
export type EmailConfigurationBasicAuth = EmailNotificationBasicAuth & EmailConfigurationCommon;
export type EmailConfigurationOAuth = EmailNotificationOAuth & EmailConfigurationCommon;

interface EmailConfigurationCommon {
  fromAddress: string;
  fromName: string;
  host: string;
  id?: string;
  port: string;
  securityProtocol: string;
  subjectPrefix: string;
  username: string;
}

interface EmailNotificationBasicAuth {
  authMethod: AuthMethod.Basic;
  basicPassword: string;
  readonly isBasicPasswordSet?: boolean;
}

interface EmailNotificationOAuth {
  authMethod: AuthMethod.OAuth;
  readonly isOauthClientIdSet?: boolean;
  readonly isOauthClientSecretSet?: boolean;
  oauthAuthenticationHost: string;
  oauthClientId: string;
  oauthClientSecret: string;
  oauthTenant: string;
}
