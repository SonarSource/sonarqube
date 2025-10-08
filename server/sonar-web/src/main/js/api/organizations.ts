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
import {Extension, MemberType, Organization, OrganizationBase, OrganizationMember, ArchivedOrganization, Paging} from "../types/types";
import { archiveRequest, deleteRequest, post, postJSON, postJSONBody, putJsonBody } from "../helpers/request";
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import axios from "axios";

export function checkOrganizationKeyExistence(key: string): Promise<Organization | undefined> {
  return getJSON(`/_codescan/organizations/${key}/exists`).then(
    r => r.exists,
    throwGlobalError
  );
}

export function getWhiteListDomains(): Promise<string[]>{
  return getJSON('/_codescan/whitelisted_domains');
}

export function createOrganization(data: OrganizationBase): Promise<Organization> {
  return postJSONBody('/_codescan/organizations', data).catch(throwGlobalError);
}

export function getOrganization(key: string): Promise<Organization> {
  return getJSON(`/_codescan/organizations/${key}`);
}

export function getUserOrganizations(): Promise<Organization[]> {
  return getJSON('/_codescan/organizations');
}

export function updateOrganization(key: string, changes: OrganizationBase) {
  return putJsonBody(`/_codescan/organizations/${key}`, {...changes }).catch(throwGlobalError);
}

export function deleteOrganization(key: string) {
  return deleteRequest(`/_codescan/organizations/${key}`).catch(throwGlobalError);
}

export function archiveOrganization(key: string) {
  return archiveRequest(`/_codescan/organizations/archive/${key}`).catch(throwGlobalError);
}

export function getArchivedOrganizations(): Promise<ArchivedOrganization[]> {
  return getJSON('/_codescan/organizations/archivedOrganizations');
}

export function restoreArchivedOrganization(key: string) {
  return putJsonBody(`/_codescan/organizations/unarchive/${key}`).catch(throwGlobalError);
}

export function toggleInviteUsersVisibility(key: string, invite_users_enabled: boolean){
  return axios.patch(`/_codescan/organizations/${key}/update_invite_users?inviteUsersEnabled=${invite_users_enabled}`).catch(throwGlobalError);
}

interface GetOrganizationNavigation {
  adminPages: Extension[];
  alm?: { key: string; membersSync: boolean; personal: boolean; url: string };
  canUpdateProjectsVisibilityToPrivate: boolean;
  isDefault: boolean;
  pages: Extension[];
}

export function getOrganizationNavigation(key: string): Promise<GetOrganizationNavigation> {
  return getJSON('/api/navigation/organization', { organization: key }).then(
    r => r.organization,
    throwGlobalError
  );
}

export function getOrganizationsThatPreventDeletion(): Promise<{
  organizations: Organization[];
}> {
  return getJSON('/api/organizations/prevent_user_deletion').catch(throwGlobalError);
}

export function searchMembers(data: {
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{ paging: Paging; users: OrganizationMember[] }> {
  return getJSON('/api/organizations/search_members', data).catch(throwGlobalError);
}

export function addMember(data: {
  login: string;
  organization: string;
  type: MemberType;
}): Promise<OrganizationMember> {
  return postJSON('/api/organizations/add_member', data).then(r => r.user, throwGlobalError);
}

export function removeMember(data: { login: string; organization: string }) {
  return post('/api/organizations/remove_member', data).catch(throwGlobalError);
}

export interface OrganizationBilling {
  nclocCount: number;
  subscription: {
    plan?: {
      maxNcloc: number;
      price: number;
    };
    nextBillingDate?: string;
    status: 'active' | 'inactive' | 'suspended';
    trial: boolean;
  };
}

export function getOrganizationBilling(organization: string): Promise<OrganizationBilling> {
  return getJSON('/api/billing/show', { organization, p: 1, ps: 1 });
}

export function setOrganizationMemberSync(data: { enabled: boolean; organization: string }) {
  return post('/api/organizations/set_members_sync', data).catch(throwGlobalError);
}

export function syncMembers(organization: string) {
  return post('/api/organizations/sync_members', { organization }).catch(throwGlobalError);
}


export function setMemberType(organization: String, login: String, type: String ): Promise<void | Response> {
  return post('/api/organizations/set_member_type', {organization, login, type}).catch(throwGlobalError);
}
