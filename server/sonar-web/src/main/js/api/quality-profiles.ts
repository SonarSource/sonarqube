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
import { getJSON } from '~sonar-aligned/helpers/request';
import { Exporter, ProfileChangelogEvent } from '../apps/quality-profiles/types';
import { csvEscape } from '../helpers/csv';
import { RequestData, post, postJSON } from '../helpers/request';
import {
  CleanCodeAttributeCategory,
  SoftwareImpact,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../types/clean-code-taxonomy';
import { QualityProfileChangelogFilterMode } from '../types/quality-profiles';
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
  organization: string;
  actions?: ProfileActions;
  activeDeprecatedRuleCount: number;
  activeRuleCount: number;
  isBuiltIn?: boolean;
  isDefault?: boolean;
  isInherited?: boolean;
  key: string;
  language: string;
  languageName: string;
  lastUsed?: string;
  name: string;
  parentKey?: string;
  parentName?: string;
  projectCount?: number;
  rulesUpdatedAt?: string;
  userUpdatedAt?: string;
}

export interface SearchQualityProfilesParameters {
  organization: string;
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
  const currentOrg: any = parameters?.organization;
  localStorage.setItem('org', currentOrg);
  return getJSON('/api/qualityprofiles/search', parameters).catch(throwGlobalError);
}

export function getQualityProfile({
  organization,
  compareToSonarWay,
  profile: { key },
}: {
  organization: string;
  compareToSonarWay?: boolean;
  profile: Profile;
}): Promise<{
  compareToSonarWay?: { missingRuleCount: number; profile: string; profileName: string };
  profile: Profile;
}> {
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
  organization,
  language,
  name: qualityProfile,
}: Pick<Profile, 'organization' | 'language' | 'name'>): Promise<{
  ancestors: ProfileInheritanceDetails[];
  children: ProfileInheritanceDetails[];
  profile: ProfileInheritanceDetails | null;
}> {
  return getJSON('/api/qualityprofiles/inheritance', {
    organization,
    language,
    qualityProfile,
  }).catch(throwGlobalError);
}

export function setDefaultProfile({ organization, language, name: qualityProfile }: Profile) {
  return post('/api/qualityprofiles/set_default', {
    organization,
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

export function deleteProfile({ organization, language, name: qualityProfile }: Profile) {
  return post('/api/qualityprofiles/delete', { organization, language, qualityProfile }).catch(throwGlobalError);
}

export function changeProfileParent(
  { organization, language, name: qualityProfile }: Profile,
  parentProfile?: Profile,
) {
  return post('/api/qualityprofiles/change_parent', {
    organization,
    language,
    qualityProfile,
    parentQualityProfile: parentProfile ? parentProfile.name : undefined,
  }).catch(throwGlobalError);
}

export function getQualityProfileExporterUrl(
  { key: exporterKey }: Exporter,
  { organization, language, name: qualityProfile }: Profile,
) {
  const queryParams = Object.entries({ organization, exporterKey, language, qualityProfile })
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

interface ChangelogData {
  filterMode: QualityProfileChangelogFilterMode;
  page?: number;
  profile: Profile;
  since: string;
  to: string;
  organization: string;
}

export function getProfileChangelog(data: ChangelogData): Promise<ChangelogResponse> {
  const {
    organization,
    filterMode,
    page,
    profile: { language, name: qualityProfile },
    since,
    to,
  } = data;
  return getJSON('/api/qualityprofiles/changelog', {
    organization,
    since,
    to,
    language,
    qualityProfile,
    filterMode,
    p: page,
  });
}

export interface RuleCompare {
  cleanCodeAttributeCategory?: CleanCodeAttributeCategory;
  impacts?: SoftwareImpact[];
  key: string;
  left?: { impacts?: SoftwareImpact[]; params?: Dict<string>; severity?: string };
  name: string;
  right?: { impacts?: SoftwareImpact[]; params?: Dict<string>; severity?: string };
}

export interface CompareResponse {
  inLeft: Array<RuleCompare>;
  inRight: Array<RuleCompare>;
  left: { name: string };
  modified: Array<RuleCompare & Required<Pick<RuleCompare, 'left' | 'right'>>>;
  right: { name: string };
}

export function compareProfiles(leftKey: string, rightKey: string): Promise<CompareResponse> {
  return getJSON('/api/qualityprofiles/compare', { leftKey, rightKey });
}

export function associateProject({ organization, language, name: qualityProfile }: Profile, project: string) {
  return post('/api/qualityprofiles/add_project', {
    organization,
    language,
    qualityProfile,
    project,
  }).catch(throwGlobalError);
}

export function dissociateProject({ organization, language, name: qualityProfile }: Profile, project: string) {
  return post('/api/qualityprofiles/remove_project', {
    organization,
    language,
    qualityProfile,
    project,
  }).catch(throwGlobalError);
}

export interface SearchUsersGroupsParameters {
  organization: string;
  language: string;
  q?: string;
  qualityProfile: string;
  selected?: 'all' | 'selected' | 'deselected';
}

interface SearchUsersResponse {
  paging: Paging;
  users: UserSelected[];
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
  organization: string;
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
  organization: string;
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
  impacts?: Record<SoftwareQuality, SoftwareImpactSeverity>;
  key: string;
  params?: Record<string, string>;
  prioritizedRule?: boolean;
  reset?: boolean;
  rule: string;
  severity?: string;
}

export function activateRule(data: ActivateRuleParameters) {
  const params =
    data.params && map(data.params, (value, key) => `${key}=${csvEscape(value)}`).join(';');
  const impacts = data.impacts && map(data.impacts, (value, key) => `${key}=${value}`).join(';');
  return post('/api/qualityprofiles/activate_rule', {
    ...data,
    params,
    impacts,
  }).catch(throwGlobalError);
}

export interface DeactivateRuleParameters {
  key: string;
  rule: string;
}

export function deactivateRule(data: DeactivateRuleParameters) {
  return post('/api/qualityprofiles/deactivate_rule', data).catch(throwGlobalError);
}
