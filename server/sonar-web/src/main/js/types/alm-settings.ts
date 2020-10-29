/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
export const enum AlmKeys {
  Azure = 'azure',
  Bitbucket = 'bitbucket',
  GitHub = 'github',
  GitLab = 'gitlab'
}

export interface AlmBindingDefinition {
  key: string;
  url?: string;
}

export interface AzureBindingDefinition extends AlmBindingDefinition {
  personalAccessToken: string;
}

export interface BitbucketBindingDefinition extends AlmBindingDefinition {
  personalAccessToken: string;
  url: string;
}

export interface GithubBindingDefinition extends AlmBindingDefinition {
  appId: string;
  clientId: string;
  clientSecret: string;
  privateKey: string;
  url: string;
}

export interface GitlabBindingDefinition extends AlmBindingDefinition {
  personalAccessToken: string;
  url?: string;
}

export interface ProjectAlmBindingResponse {
  alm: AlmKeys;
  key: string;
  repository?: string;
  slug?: string;
  summaryCommentEnabled?: boolean;
}

export interface ProjectBitbucketBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.Bitbucket;
  repository: string;
  slug: string;
}

export interface ProjectGitHubBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.GitHub;
  repository: string;
}

export interface ProjectGitLabBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.GitLab;
  repository: string;
  url: string;
}

export interface ProjectAlmBindingParams {
  almSetting: string;
  project: string;
}

export interface AzureProjectAlmBindingParams extends ProjectAlmBindingParams {}

export interface BitbucketProjectAlmBindingParams extends ProjectAlmBindingParams {
  repository: string;
  slug: string;
}

export interface GithubProjectAlmBindingParams extends ProjectAlmBindingParams {
  repository: string;
  summaryCommentEnabled: boolean;
}

export interface GitlabProjectAlmBindingParams extends ProjectAlmBindingParams {
  repository?: string;
}

export interface AlmSettingsInstance {
  alm: AlmKeys;
  key: string;
  url?: string;
}

export interface AlmSettingsBindingDefinitions {
  [AlmKeys.Azure]: AzureBindingDefinition[];
  [AlmKeys.Bitbucket]: BitbucketBindingDefinition[];
  [AlmKeys.GitHub]: GithubBindingDefinition[];
  [AlmKeys.GitLab]: GitlabBindingDefinition[];
}

export interface AlmSettingsBindingStatus {
  alertSuccess: boolean;
  failureMessage: string;
  type: AlmSettingsBindingStatusType;
}

export enum AlmSettingsBindingStatusType {
  Validating,
  Success,
  Failure,
  Warning
}
