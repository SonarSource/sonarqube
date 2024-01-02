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
export const enum AlmKeys {
  Azure = 'azure',
  BitbucketServer = 'bitbucket',
  BitbucketCloud = 'bitbucketcloud',
  GitHub = 'github',
  GitLab = 'gitlab',
}

export type AlmBindingDefinition =
  | AzureBindingDefinition
  | GithubBindingDefinition
  | GitlabBindingDefinition
  | BitbucketServerBindingDefinition
  | BitbucketCloudBindingDefinition;

export interface AlmBindingDefinitionBase {
  key: string;
  url?: string;
}

export interface AzureBindingDefinition extends AlmBindingDefinitionBase {
  personalAccessToken: string;
  url?: string;
}

export interface BitbucketServerBindingDefinition extends AlmBindingDefinitionBase {
  personalAccessToken: string;
  url: string;
}

export interface BitbucketCloudBindingDefinition extends AlmBindingDefinitionBase {
  clientId: string;
  clientSecret: string;
  workspace: string;
}

export interface GithubBindingDefinition extends AlmBindingDefinitionBase {
  appId: string;
  clientId: string;
  clientSecret: string;
  privateKey: string;
  url: string;
  webhookSecret: string;
}

export interface GitlabBindingDefinition extends AlmBindingDefinitionBase {
  personalAccessToken: string;
  url?: string;
}

export interface ProjectAlmBindingResponse {
  alm: AlmKeys;
  key: string;
  repository?: string;
  slug?: string;
  summaryCommentEnabled?: boolean;
  monorepo: boolean;
}

export interface ProjectAzureBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.Azure;
  repository: string;
  slug: string;
  url: string;
}

export interface ProjectBitbucketBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.BitbucketServer;
  repository: string;
  slug: string;
}

export interface ProjectBitbucketCloudBindingResponse extends ProjectAlmBindingResponse {
  alm: AlmKeys.BitbucketCloud;
  repository: string;
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
  monorepo: boolean;
}

export interface AzureProjectAlmBindingParams extends ProjectAlmBindingParams {
  projectName: string;
  repositoryName: string;
}

export interface BitbucketProjectAlmBindingParams extends ProjectAlmBindingParams {
  repository: string;
  slug: string;
}

export interface BitbucketCloudProjectAlmBindingParams extends ProjectAlmBindingParams {
  repository: string;
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
  [AlmKeys.BitbucketServer]: BitbucketServerBindingDefinition[];
  [AlmKeys.BitbucketCloud]: BitbucketCloudBindingDefinition[];
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
  Warning,
}

export enum ProjectAlmBindingConfigurationErrorScope {
  Global = 'GLOBAL',
  Project = 'PROJECT',
  Unknown = 'UNKNOWN',
}

export interface ProjectAlmBindingConfigurationErrors {
  scope: ProjectAlmBindingConfigurationErrorScope;
  errors: { msg: string }[];
}

export function isProjectBitbucketBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectBitbucketBindingResponse {
  return binding.alm === AlmKeys.BitbucketServer;
}

export function isProjectBitbucketCloudBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectBitbucketBindingResponse {
  return binding.alm === AlmKeys.BitbucketCloud;
}

export function isProjectGitHubBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectGitHubBindingResponse {
  return binding.alm === AlmKeys.GitHub;
}

export function isProjectGitLabBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectGitLabBindingResponse {
  return binding.alm === AlmKeys.GitLab;
}

export function isProjectAzureBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectAzureBindingResponse {
  return binding.alm === AlmKeys.Azure;
}

export function isBitbucketBindingDefinition(
  binding?: AlmBindingDefinitionBase & { url?: string }
): binding is BitbucketServerBindingDefinition {
  return binding !== undefined && binding.url !== undefined;
}

export function isBitbucketCloudBindingDefinition(
  binding?: AlmBindingDefinitionBase & { clientId?: string; workspace?: string }
): binding is BitbucketCloudBindingDefinition {
  return binding !== undefined && binding.clientId !== undefined && binding.workspace !== undefined;
}

export function isGithubBindingDefinition(
  binding?: AlmBindingDefinitionBase & { appId?: string; url?: string }
): binding is GithubBindingDefinition {
  return binding !== undefined && binding.appId !== undefined && binding.url !== undefined;
}

export function isGitLabBindingDefinition(
  binding?: AlmBindingDefinitionBase | GithubBindingDefinition | BitbucketCloudBindingDefinition
): binding is GitlabBindingDefinition {
  // There's too much overlap with the others. We must not only validate that certain fields are
  // present, we must also validate that others are NOT present. And even so, we cannot be 100%
  // sure, as right now, Azure, Bitbucket Server, and GitLab have the same signature.
  return (
    binding !== undefined &&
    (binding as GithubBindingDefinition).appId === undefined &&
    (binding as BitbucketCloudBindingDefinition).workspace === undefined
  );
}
