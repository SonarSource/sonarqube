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
import { map } from 'lodash';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { Exporter, ProfileChangelogEvent } from '../apps/quality-profiles/types';
import { csvEscape } from '../helpers/csv';
import { RequestData, getJSON, post, postJSON } from '../helpers/request';
import { CleanCodeAttributeCategory, SoftwareImpact } from '../types/clean-code-taxonomy';
import { Dict, Paging, ProfileInheritanceDetails, UserSelected } from '../types/types';

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
  isBuiltIn?: boolean;
  projectCount?: number;
}

export interface SearchQualityProfilesParameters {
  defaults?: boolean;
  language?: string;
  project?: string;
  qualityProfile?: string;
}

export interface SearchQualityProfilesResponse {
  actions?: Actions;
  profiles: Profile[];
}

export function searchQualityProfiles(
  parameters?: SearchQualityProfilesParameters,
): Promise<SearchQualityProfilesResponse> {
  return getJSON('/api/qualityprofiles/search', parameters).catch(throwGlobalError);
}

export function getQualityProfile({
  compareToSonarWay,
  profile: { key },
}: {
  compareToSonarWay?: boolean;
  profile: Profile;
}): Promise<any> {
  return getJSON('/api/qualityprofiles/show', { compareToSonarWay, key });
}

export function createQualityProfile(data: RequestData): Promise<any> {
  return postJSON('/api/qualityprofiles/create', data).catch(throwGlobalError);
}

export function restoreQualityProfile(data: RequestData): Promise<any> {
  return postJSON('/api/qualityprofiles/restore', data).catch(throwGlobalError);
}

export interface ProfileProject {
  key: string;
  name: string;
  selected: boolean;
}

export function getProfileProjects(
  data: RequestData,
): Promise<{ more: boolean; paging: Paging; results: ProfileProject[] }> {
  return getJSON('/api/qualityprofiles/projects', data).catch(throwGlobalError);
}

export function getProfileInheritance({
  language,
  name: qualityProfile,
}: Pick<Profile, 'language' | 'name'>): Promise<{
  ancestors: ProfileInheritanceDetails[];
  children: ProfileInheritanceDetails[];
  profile: ProfileInheritanceDetails;
}> {
  return getJSON('/api/qualityprofiles/inheritance', {
    language,
    qualityProfile,
  }).catch(throwGlobalError);
}

export function setDefaultProfile({ language, name: qualityProfile }: Profile) {
  return post('/api/qualityprofiles/set_default', {
    language,
    qualityProfile,
  });
}

export function renameProfile(key: string, name: string) {
  return post('/api/qualityprofiles/rename', { key, name }).catch(throwGlobalError);
}

export function copyProfile(fromKey: string, name: string): Promise<Profile> {
  return postJSON('/api/qualityprofiles/copy', { fromKey, toName: name }).catch(throwGlobalError);
}

export function deleteProfile({ language, name: qualityProfile }: Profile) {
  return post('/api/qualityprofiles/delete', { language, qualityProfile }).catch(throwGlobalError);
}

export function changeProfileParent(
  { language, name: qualityProfile }: Profile,
  parentProfile?: Profile,
) {
  return post('/api/qualityprofiles/change_parent', {
    language,
    qualityProfile,
    parentQualityProfile: parentProfile ? parentProfile.name : undefined,
  }).catch(throwGlobalError);
}

export function getQualityProfileExporterUrl(
  { key: exporterKey }: Exporter,
  { language, name: qualityProfile }: Profile,
) {
  const queryParams = Object.entries({ exporterKey, language, qualityProfile })
    .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
    .join('&');
  return `/api/qualityprofiles/export?${queryParams}`;
}

export function getImporters(): Promise<
  Array<{ key: string; languages: Array<string>; name: string }>
> {
  return getJSON('/api/qualityprofiles/importers').then((r) => r.importers, throwGlobalError);
}

export function getExporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/exporters').then((r) => r.exporters);
}

export interface ChangelogResponse {
  events: ProfileChangelogEvent[];
  paging: Paging;
}

export function getProfileChangelog(
  since: any,
  to: any,
  { language, name: qualityProfile }: Profile,
  page?: number,
): Promise<ChangelogResponse> {
  return getJSON('/api/qualityprofiles/changelog', {
    since,
    to,
    language,
    qualityProfile,
    p: page,
  });
}

export interface RuleCompare {
  key: string;
  name: string;
  cleanCodeAttributeCategory?: CleanCodeAttributeCategory;
  impacts: SoftwareImpact[];
  left?: { params: Dict<string>; severity: string };
  right?: { params: Dict<string>; severity: string };
}

export interface CompareResponse {
  left: { name: string };
  right: { name: string };
  inLeft: Array<RuleCompare>;
  inRight: Array<RuleCompare>;
  modified: Array<
    RuleCompare & {
      left: { params: Dict<string>; severity: string };
      right: { params: Dict<string>; severity: string };
    }
  >;
}

export function compareProfiles(leftKey: string, rightKey: string): Promise<CompareResponse> {
  return getJSON('/api/qualityprofiles/compare', { leftKey, rightKey });
}

export function associateProject({ language, name: qualityProfile }: Profile, project: string) {
  return post('/api/qualityprofiles/add_project', {
    language,
    qualityProfile,
    project,
  }).catch(throwGlobalError);
}

export function dissociateProject({ language, name: qualityProfile }: Profile, project: string) {
  return post('/api/qualityprofiles/remove_project', {
    language,
    qualityProfile,
    project,
  }).catch(throwGlobalError);
}

export interface SearchUsersGroupsParameters {
  language: string;
  qualityProfile: string;
  q?: string;
  selected?: 'all' | 'selected' | 'deselected';
}

interface SearchUsersResponse {
  users: UserSelected[];
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
  parameters: SearchUsersGroupsParameters,
): Promise<SearchGroupsResponse> {
  return getJSON('/api/qualityprofiles/search_groups', parameters).catch(throwGlobalError);
}

export interface AddRemoveUserParameters {
  language: string;
  login: string;
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

export interface ActivateRuleParameters {
  key: string;
  params?: Dict<string>;
  reset?: boolean;
  rule: string;
  severity?: string;
}

export function activateRule(data: ActivateRuleParameters) {
  const params =
    data.params && map(data.params, (value, key) => `${key}=${csvEscape(value)}`).join(';');
  return post('/api/qualityprofiles/activate_rule', { ...data, params }).catch(throwGlobalError);
}

export interface DeactivateRuleParameters {
  key: string;
  rule: string;
}

export function deactivateRule(data: DeactivateRuleParameters) {
  return post('/api/qualityprofiles/deactivate_rule', data).catch(throwGlobalError);
}
