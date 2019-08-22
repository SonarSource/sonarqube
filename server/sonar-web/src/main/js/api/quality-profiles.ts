/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { csvEscape } from 'sonar-ui-common/helpers/csv';
import { getJSON, post, postJSON, RequestData } from 'sonar-ui-common/helpers/request';
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
  return postJSON('/api/qualityprofiles/create', data).catch(throwGlobalError);
}

export function restoreQualityProfile(data: RequestData): Promise<any> {
  return postJSON('/api/qualityprofiles/restore', data).catch(throwGlobalError);
}

export interface ProfileProject {
  id: number;
  key: string;
  name: string;
  selected: boolean;
}

export function getProfileProjects(
  data: RequestData
): Promise<{ more: boolean; paging: T.Paging; results: ProfileProject[] }> {
  return getJSON('/api/qualityprofiles/projects', data).catch(throwGlobalError);
}

export function getProfileInheritance(
  profileKey: string
): Promise<{
  ancestors: T.ProfileInheritanceDetails[];
  children: T.ProfileInheritanceDetails[];
  profile: T.ProfileInheritanceDetails;
}> {
  return getJSON('/api/qualityprofiles/inheritance', { profileKey }).catch(throwGlobalError);
}

export function setDefaultProfile(profileKey: string) {
  return post('/api/qualityprofiles/set_default', { profileKey });
}

export function renameProfile(key: string, name: string) {
  return post('/api/qualityprofiles/rename', { key, name }).catch(throwGlobalError);
}

export function copyProfile(fromKey: string, toName: string): Promise<any> {
  return postJSON('/api/qualityprofiles/copy', { fromKey, toName }).catch(throwGlobalError);
}

export function deleteProfile(profileKey: string) {
  return post('/api/qualityprofiles/delete', { profileKey }).catch(throwGlobalError);
}

export function changeProfileParent(profileKey: string, parentKey: string) {
  return post('/api/qualityprofiles/change_parent', {
    profileKey,
    parentKey
  }).catch(throwGlobalError);
}

export function getImporters(): Promise<
  Array<{ key: string; languages: Array<string>; name: string }>
> {
  return getJSON('/api/qualityprofiles/importers').then(r => r.importers, throwGlobalError);
}

export function getExporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/exporters').then(r => r.exporters);
}

export function getProfileChangelog(data: RequestData): Promise<any> {
  return getJSON('/api/qualityprofiles/changelog', data);
}

export interface CompareResponse {
  left: { name: string };
  right: { name: string };
  inLeft: Array<{ key: string; name: string; severity: string }>;
  inRight: Array<{ key: string; name: string; severity: string }>;
  modified: Array<{
    key: string;
    name: string;
    left: { params: T.Dict<string>; severity: string };
    right: { params: T.Dict<string>; severity: string };
  }>;
}

export function compareProfiles(leftKey: string, rightKey: string): Promise<CompareResponse> {
  return getJSON('/api/qualityprofiles/compare', { leftKey, rightKey });
}

export function associateProject(key: string, project: string) {
  return post('/api/qualityprofiles/add_project', { key, project }).catch(throwGlobalError);
}

export function dissociateProject(key: string, project: string) {
  return post('/api/qualityprofiles/remove_project', { key, project }).catch(throwGlobalError);
}

export interface SearchUsersGroupsParameters {
  language: string;
  organization?: string;
  qualityProfile: string;
  q?: string;
  selected?: 'all' | 'selected' | 'deselected';
}

interface SearchUsersResponse {
  users: T.UserSelected[];
  paging: T.Paging;
}

export function searchUsers(parameters: SearchUsersGroupsParameters): Promise<SearchUsersResponse> {
  return getJSON('/api/qualityprofiles/search_users', parameters).catch(throwGlobalError);
}

export interface SearchGroupsResponse {
  groups: Array<{ name: string }>;
  paging: T.Paging;
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
  params?: T.Dict<string>;
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
