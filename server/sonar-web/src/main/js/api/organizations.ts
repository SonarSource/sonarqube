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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getOrganizations(data: {
  organizations?: string;
  member?: boolean;
}): Promise<{
  organizations: T.Organization[];
  paging: T.Paging;
}> {
  return getJSON('/api/organizations/search', data).catch(throwGlobalError);
}

export function getOrganization(key: string): Promise<T.Organization | undefined> {
  return getJSON('/api/organizations/search', { organizations: key }).then(
    r => r.organizations.find((o: T.Organization) => o.key === key),
    throwGlobalError
  );
}

interface GetOrganizationNavigation {
  adminPages: T.Extension[];
  alm?: { key: string; membersSync: boolean; personal: boolean; url: string };
  canUpdateProjectsVisibilityToPrivate: boolean;
  isDefault: boolean;
  pages: T.Extension[];
}

export function getOrganizationNavigation(key: string): Promise<GetOrganizationNavigation> {
  return getJSON('/api/navigation/organization', { organization: key }).then(
    r => r.organization,
    throwGlobalError
  );
}

export function getOrganizationsThatPreventDeletion(): Promise<{
  organizations: T.Organization[];
}> {
  return getJSON('/api/organizations/prevent_user_deletion').catch(throwGlobalError);
}

export function createOrganization(
  data: T.OrganizationBase & { installationId?: string }
): Promise<T.Organization> {
  return postJSON('/api/organizations/create', data).then(r => r.organization, throwGlobalError);
}

export function updateOrganization(key: string, changes: T.OrganizationBase) {
  return post('/api/organizations/update', { key, ...changes }).catch(throwGlobalError);
}

export function deleteOrganization(key: string) {
  return post('/api/organizations/delete', { key }).catch(throwGlobalError);
}

export function searchMembers(data: {
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{ paging: T.Paging; users: T.OrganizationMember[] }> {
  return getJSON('/api/organizations/search_members', data).catch(throwGlobalError);
}

export function addMember(data: {
  login: string;
  organization: string;
}): Promise<T.OrganizationMember> {
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
