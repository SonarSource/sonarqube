/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { keyBy } from 'lodash';
import { throwGlobalError } from '../helpers/error';
import { getJSON, post, postJSON } from '../helpers/request';
import {
  GitHubConfigurationStatus,
  GitHubMapping,
  GitLabConfigurationCreateBody,
  GitLabConfigurationUpdateBody,
  GithubStatus,
  GitlabConfiguration,
  ProvisioningType,
} from '../types/provisioning';
import { Paging } from '../types/types';
import { getValues, resetSettingValue, setSimpleSettingValue } from './settings';

const GITHUB_PERMISSION_MAPPINGS = '/api/v2/dop-translation/github-permission-mappings';

export function fetchIsScimEnabled(): Promise<boolean> {
  return getJSON('/api/scim_management/status')
    .then((r) => r.enabled)
    .catch(throwGlobalError);
}

export function activateScim(): Promise<void> {
  return post('/api/scim_management/enable').catch(throwGlobalError);
}

export function deactivateScim(): Promise<void> {
  return post('/api/scim_management/disable').catch(throwGlobalError);
}

export function fetchGithubProvisioningStatus(): Promise<GithubStatus> {
  return getJSON('/api/github_provisioning/status').catch(throwGlobalError);
}

export function activateGithubProvisioning(): Promise<void> {
  return post('/api/github_provisioning/enable').catch(throwGlobalError);
}

export function deactivateGithubProvisioning(): Promise<void> {
  return post('/api/github_provisioning/disable').catch(throwGlobalError);
}

export function checkConfigurationValidity(): Promise<GitHubConfigurationStatus> {
  return postJSON('/api/github_provisioning/check').catch(throwGlobalError);
}

export function syncNowGithubProvisioning(): Promise<void> {
  return post('/api/github_provisioning/sync').catch(throwGlobalError);
}

export function fetchGithubRolesMapping() {
  return axios
    .get<{ githubPermissionsMappings: GitHubMapping[] }>(GITHUB_PERMISSION_MAPPINGS)
    .then((data) => data.githubPermissionsMappings);
}

export function updateGithubRolesMapping(
  role: string,
  data: Partial<Pick<GitHubMapping, 'permissions'>>,
) {
  return axios.patch<GitHubMapping>(
    `${GITHUB_PERMISSION_MAPPINGS}/${encodeURIComponent(role)}`,
    data,
  );
}

export function addGithubRolesMapping(data: Omit<GitHubMapping, 'id'>) {
  return axios.post<GitHubMapping>(GITHUB_PERMISSION_MAPPINGS, data);
}

export function deleteGithubRolesMapping(role: string) {
  return axios.delete(`${GITHUB_PERMISSION_MAPPINGS}/${encodeURIComponent(role)}`);
}

const GITLAB_SETTING_ENABLED = 'sonar.auth.gitlab.enabled';
const GITLAB_SETTING_URL = 'sonar.auth.gitlab.url';
const GITLAB_SETTING_APP_ID = 'sonar.auth.gitlab.applicationId.secured';
const GITLAB_SETTING_SECRET = 'sonar.auth.gitlab.secret.secured';
export const GITLAB_SETTING_ALLOW_SIGNUP = 'sonar.auth.gitlab.allowUsersToSignUp';
const GITLAB_SETTING_GROUPS_SYNC = 'sonar.auth.gitlab.groupsSync';
const GITLAB_SETTING_PROVISIONING_ENABLED = 'provisioning.gitlab.enabled';
export const GITLAB_SETTING_GROUP_TOKEN = 'provisioning.gitlab.token.secured';
export const GITLAB_SETTING_GROUPS = 'provisioning.gitlab.groups';

const gitlabKeys = [
  GITLAB_SETTING_ENABLED,
  GITLAB_SETTING_URL,
  GITLAB_SETTING_APP_ID,
  GITLAB_SETTING_SECRET,
  GITLAB_SETTING_ALLOW_SIGNUP,
  GITLAB_SETTING_GROUPS_SYNC,
  GITLAB_SETTING_PROVISIONING_ENABLED,
  GITLAB_SETTING_GROUP_TOKEN,
  GITLAB_SETTING_GROUPS,
];

const fieldKeyMap = {
  enabled: GITLAB_SETTING_ENABLED,
  url: GITLAB_SETTING_URL,
  applicationId: GITLAB_SETTING_APP_ID,
  clientSecret: GITLAB_SETTING_SECRET,
  allowUsersToSignUp: GITLAB_SETTING_ALLOW_SIGNUP,
  synchronizeUserGroups: GITLAB_SETTING_GROUPS_SYNC,
  type: GITLAB_SETTING_PROVISIONING_ENABLED,
  provisioningToken: GITLAB_SETTING_GROUP_TOKEN,
  groups: GITLAB_SETTING_GROUPS,
};

const getGitLabConfiguration = async (): Promise<GitlabConfiguration | null> => {
  const values = await getValues({
    keys: gitlabKeys,
  });
  const valuesMap = keyBy(values, 'key');
  if (!valuesMap[GITLAB_SETTING_APP_ID] || !valuesMap[GITLAB_SETTING_SECRET]) {
    return null;
  }
  return {
    id: '1',
    enabled: valuesMap[GITLAB_SETTING_ENABLED]?.value === 'true',
    url: valuesMap[GITLAB_SETTING_URL]?.value ?? 'https://gitlab.com',
    synchronizeUserGroups: valuesMap[GITLAB_SETTING_GROUPS_SYNC]?.value === 'true',
    type:
      valuesMap[GITLAB_SETTING_PROVISIONING_ENABLED]?.value === 'true'
        ? ProvisioningType.auto
        : ProvisioningType.jit,
    groups: valuesMap[GITLAB_SETTING_GROUPS]?.values
      ? valuesMap[GITLAB_SETTING_GROUPS]?.values
      : [],
    allowUsersToSignUp: valuesMap[GITLAB_SETTING_ALLOW_SIGNUP]?.value === 'true',
  };
};

export async function fetchGitLabConfigurations(): Promise<{
  configurations: GitlabConfiguration[];
  page: Paging;
}> {
  const config = await getGitLabConfiguration();
  return {
    configurations: config ? [config] : [],
    page: {
      pageIndex: 1,
      pageSize: 1,
      total: config ? 1 : 0,
    },
  };
}

export async function fetchGitLabConfiguration(_id: string): Promise<GitlabConfiguration> {
  const configuration = await getGitLabConfiguration();
  if (!configuration) {
    return Promise.reject(new Error('GitLab configuration not found'));
  }
  return Promise.resolve(configuration);
}

export async function createGitLabConfiguration(
  configuration: GitLabConfigurationCreateBody,
): Promise<GitlabConfiguration> {
  await Promise.all(
    Object.entries(configuration).map(
      ([key, value]: [key: keyof GitLabConfigurationCreateBody, value: string]) =>
        setSimpleSettingValue({ key: fieldKeyMap[key], value }),
    ),
  );
  await setSimpleSettingValue({ key: fieldKeyMap.enabled, value: 'true' });
  return fetchGitLabConfiguration('');
}

export async function updateGitLabConfiguration(
  _id: string,
  configuration: Partial<GitLabConfigurationUpdateBody>,
): Promise<GitlabConfiguration> {
  await Promise.all(
    Object.entries(configuration).map(
      ([key, value]: [key: keyof typeof fieldKeyMap, value: string | string[]]) => {
        if (fieldKeyMap[key] === GITLAB_SETTING_PROVISIONING_ENABLED) {
          return setSimpleSettingValue({
            key: fieldKeyMap[key],
            value: value === ProvisioningType.auto ? 'true' : 'false',
          });
        } else if (typeof value === 'boolean') {
          return setSimpleSettingValue({ key: fieldKeyMap[key], value: value ? 'true' : 'false' });
        } else if (Array.isArray(value)) {
          return setSimpleSettingValue({ key: fieldKeyMap[key], values: value });
        }
        return setSimpleSettingValue({ key: fieldKeyMap[key], value });
      },
    ),
  );
  return fetchGitLabConfiguration('');
}

export function deleteGitLabConfiguration(_id: string): Promise<void> {
  return resetSettingValue({ keys: gitlabKeys.join(',') });
}
