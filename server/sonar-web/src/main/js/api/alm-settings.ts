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
import { getJSON, post } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import {
  AlmSettingsBindingDefinitions,
  AlmSettingsInstance,
  AzureBindingDefinition,
  AzureProjectAlmBinding,
  BitbucketBindingDefinition,
  BitbucketProjectAlmBinding,
  GithubBindingDefinition,
  GithubProjectAlmBinding,
  GitlabBindingDefinition,
  GitlabProjectAlmBinding,
  ProjectAlmBinding
} from '../types/alm-settings';

export function getAlmDefinitions(): Promise<AlmSettingsBindingDefinitions> {
  return getJSON('/api/alm_settings/list_definitions').catch(throwGlobalError);
}

export function getAlmSettings(project?: string): Promise<AlmSettingsInstance[]> {
  return getJSON('/api/alm_settings/list', { project })
    .then(({ almSettings }) => almSettings)
    .catch(throwGlobalError);
}

export function createGithubConfiguration(data: GithubBindingDefinition) {
  return post('/api/alm_settings/create_github', data).catch(throwGlobalError);
}

export function updateGithubConfiguration(data: GithubBindingDefinition & { newKey: string }) {
  return post('/api/alm_settings/update_github', data).catch(throwGlobalError);
}

export function createAzureConfiguration(data: AzureBindingDefinition) {
  return post('/api/alm_settings/create_azure', data).catch(throwGlobalError);
}

export function updateAzureConfiguration(data: AzureBindingDefinition & { newKey: string }) {
  return post('/api/alm_settings/update_azure', data).catch(throwGlobalError);
}

export function createBitbucketConfiguration(data: BitbucketBindingDefinition) {
  return post('/api/alm_settings/create_bitbucket', data).catch(throwGlobalError);
}

export function updateBitbucketConfiguration(
  data: BitbucketBindingDefinition & { newKey: string }
) {
  return post('/api/alm_settings/update_bitbucket', data).catch(throwGlobalError);
}

export function createGitlabConfiguration(data: GitlabBindingDefinition) {
  return post('/api/alm_settings/create_gitlab', data).catch(throwGlobalError);
}

export function updateGitlabConfiguration(data: GitlabBindingDefinition & { newKey: string }) {
  return post('/api/alm_settings/update_gitlab', data).catch(throwGlobalError);
}

export function deleteConfiguration(key: string) {
  return post('/api/alm_settings/delete', { key }).catch(throwGlobalError);
}

export function countBindedProjects(almSetting: string) {
  return getJSON('/api/alm_settings/count_binding', { almSetting })
    .then(({ projects }) => projects)
    .catch(throwGlobalError);
}

export function getProjectAlmBinding(project: string): Promise<ProjectAlmBinding> {
  return getJSON('/api/alm_settings/get_binding', { project });
}

export function deleteProjectAlmBinding(project: string): Promise<void> {
  return post('/api/alm_settings/delete_binding', { project }).catch(throwGlobalError);
}

export function setProjectAzureBinding(data: AzureProjectAlmBinding) {
  return post('/api/alm_settings/set_azure_binding', data).catch(throwGlobalError);
}

export function setProjectBitbucketBinding(data: BitbucketProjectAlmBinding) {
  return post('/api/alm_settings/set_bitbucket_binding', data).catch(throwGlobalError);
}

export function setProjectGithubBinding(data: GithubProjectAlmBinding) {
  return post('/api/alm_settings/set_github_binding', data).catch(throwGlobalError);
}

export function setProjectGitlabBinding(data: GitlabProjectAlmBinding) {
  return post('/api/alm_settings/set_gitlab_binding', data).catch(throwGlobalError);
}
