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
import { map } from 'lodash';
import { csvEscape } from '../helpers/csv';
import {
  request,
  checkStatus,
  parseJSON,
  getJSON,
  post,
  postJSON,
  RequestData
} from '../helpers/request';
import { Paging } from '../app/types';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface ProfileActions {
  associateProjects?: boolean;
  copy?: boolean;
  delete?: boolean;
  edit?: boolean;
  setAsDefault?: boolean;
}

export interface Actions {
  create?: boolean;
}

export interface Profile {
  actions?: ProfileActions;
  key: string;
  name: string;
  language: string;
  languageName: string;
  isInherited?: boolean;
  parentKey?: string;
  parentName?: string;
  isDefault?: boolean;
  activeRuleCount: number;
  activeDeprecatedRuleCount: number;
  rulesUpdatedAt?: string;
  lastUsed?: string;
  userUpdatedAt?: string;
  organization: string;
  isBuiltIn?: boolean;
  projectCount?: number;
}

export interface SearchQualityProfilesParameters {
  defaults?: boolean;
  language?: string;
  organization?: string;
  project?: string;
  qualityProfile?: string;
}

export interface SearchQualityProfilesResponse {
  actions?: Actions;
  profiles: Profile[];
}

export function searchQualityProfiles(
  parameters: SearchQualityProfilesParameters
): Promise<SearchQualityProfilesResponse> {
  return getJSON('/api/qualityprofiles/search', parameters).catch(throwGlobalError);
}

export function getQualityProfile(data: {
  compareToSonarWay?: boolean;
  profile: string;
}): Promise<any> {
  return getJSON('/api/qualityprofiles/show', data);
}

export function createQualityProfile(data: RequestData): Promise<any> {
  return request('/api/qualityprofiles/create')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function restoreQualityProfile(data: RequestData): Promise<any> {
  return request('/api/qualityprofiles/restore')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function getProfileProjects(data: RequestData): Promise<any> {
  return getJSON('/api/qualityprofiles/projects', data).catch(throwGlobalError);
}

export function getProfileInheritance(profileKey: string): Promise<any> {
  return getJSON('/api/qualityprofiles/inheritance', { profileKey });
}

export function setDefaultProfile(profileKey: string): Promise<void> {
  return post('/api/qualityprofiles/set_default', { profileKey });
}

export function renameProfile(key: string, name: string): Promise<void> {
  return post('/api/qualityprofiles/rename', { key, name });
}

export function copyProfile(fromKey: string, toName: string): Promise<any> {
  return postJSON('/api/qualityprofiles/copy', { fromKey, toName });
}

export function deleteProfile(profileKey: string): Promise<void> {
  return post('/api/qualityprofiles/delete', { profileKey });
}

export function changeProfileParent(profileKey: string, parentKey: string): Promise<void> {
  return post('/api/qualityprofiles/change_parent', { profileKey, parentKey });
}

export function getImporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/importers').then(r => r.importers);
}

export function getExporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/exporters').then(r => r.exporters);
}

export function getProfileChangelog(data: RequestData): Promise<any> {
  return getJSON('/api/qualityprofiles/changelog', data);
}

export function compareProfiles(leftKey: string, rightKey: string): Promise<any> {
  return getJSON('/api/qualityprofiles/compare', { leftKey, rightKey });
}

export function associateProject(profileKey: string, projectKey: string): Promise<void> {
  return post('/api/qualityprofiles/add_project', { profileKey, projectKey });
}

export function dissociateProject(profileKey: string, projectKey: string): Promise<void> {
  return post('/api/qualityprofiles/remove_project', { profileKey, projectKey });
}

export interface SearchUsersGroupsParameters {
  language: string;
  organization?: string;
  qualityProfile: string;
  q?: string;
  selected?: 'all' | 'selected' | 'deselected';
}

export interface SearchUsersResponse {
  users: Array<{
    avatar?: string;
    login: string;
    name: string;
    selected?: boolean;
  }>;
  paging: Paging;
}

export function searchUsers(parameters: SearchUsersGroupsParameters): Promise<SearchUsersResponse> {
  return getJSON('/api/qualityprofiles/search_users', parameters).catch(throwGlobalError);
}

export interface SearchGroupsResponse {
  groups: Array<{ name: string }>;
  paging: Paging;
}

export function searchGroups(
  parameters: SearchUsersGroupsParameters
): Promise<SearchGroupsResponse> {
  return getJSON('/api/qualityprofiles/search_groups', parameters).catch(throwGlobalError);
}

export interface AddRemoveUserParameters {
  language: string;
  login: string;
  organization?: string;
  qualityProfile: string;
}

export function addUser(parameters: AddRemoveUserParameters): Promise<void | Response> {
  return post('/api/qualityprofiles/add_user', parameters).catch(throwGlobalError);
}

export function removeUser(parameters: AddRemoveUserParameters): Promise<void | Response> {
  return post('/api/qualityprofiles/remove_user', parameters).catch(throwGlobalError);
}

export interface AddRemoveGroupParameters {
  group: string;
  language: string;
  organization?: string;
  qualityProfile: string;
}

export function addGroup(parameters: AddRemoveGroupParameters): Promise<void | Response> {
  return post('/api/qualityprofiles/add_group', parameters).catch(throwGlobalError);
}

export function removeGroup(parameters: AddRemoveGroupParameters): Promise<void | Response> {
  return post('/api/qualityprofiles/remove_group', parameters).catch(throwGlobalError);
}

export interface BulkActivateParameters {
  /* eslint-disable camelcase */
  activation?: boolean;
  active_severities?: string;
  asc?: boolean;
  available_since?: string;
  compareToProfile?: string;
  inheritance?: string;
  is_template?: string;
  languages?: string;
  organization: string | undefined;
  q?: string;
  qprofile?: string;
  repositories?: string;
  rule_key?: string;
  s?: string;
  severities?: string;
  statuses?: string;
  tags?: string;
  targetKey: string;
  targetSeverity?: string;
  template_key?: string;
  types?: string;
  /* eslint-enable camelcase */
}

export function bulkActivateRules(data: BulkActivateParameters) {
  return postJSON('/api/qualityprofiles/activate_rules', data);
}

export function bulkDeactivateRules(data: BulkActivateParameters) {
  return postJSON('/api/qualityprofiles/deactivate_rules', data);
}

export function activateRule(data: {
  key: string;
  organization: string | undefined;
  params?: { [key: string]: string };
  reset?: boolean;
  rule: string;
  severity?: string;
}) {
  const params =
    data.params && map(data.params, (value, key) => `${key}=${csvEscape(value)}`).join(';');
  return post('/api/qualityprofiles/activate_rule', { ...data, params }).catch(throwGlobalError);
}

export function deactivateRule(data: {
  key: string;
  organization: string | undefined;
  rule: string;
}) {
  return post('/api/qualityprofiles/deactivate_rule', data).catch(throwGlobalError);
}
