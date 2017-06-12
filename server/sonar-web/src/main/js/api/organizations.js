/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { getJSON, post, postJSON } from '../helpers/request';
import type { Organization } from '../store/organizations/duck';
import throwGlobalError from '../app/utils/throwGlobalError';

export const getOrganizations = (organizations?: Array<string>) => {
  const data = {};
  if (organizations) {
    Object.assign(data, { organizations: organizations.join() });
  }
  return getJSON('/api/organizations/search', data);
};

export const getMyOrganizations = () =>
  getJSON('/api/organizations/search_my_organizations').then(r => r.organizations);

type GetOrganizationType = null | Organization;

type GetOrganizationNavigation = {
  canAdmin: boolean,
  canDelete: boolean,
  canProvisionProjects: boolean,
  isDefault: boolean,
  pages: Array<{ key: string, name: string }>,
  adminPages: Array<{ key: string, name: string }>
};

export const getOrganization = (key: string): Promise<GetOrganizationType> => {
  return getOrganizations([key])
    .then(r => r.organizations.find(o => o.key === key))
    .catch(throwGlobalError);
};

export const getOrganizationNavigation = (key: string): Promise<GetOrganizationNavigation> => {
  return getJSON('/api/navigation/organization', { organization: key }).then(r => r.organization);
};

export const createOrganization = (fields: {}): Promise<Organization> =>
  postJSON('/api/organizations/create', fields).then(r => r.organization, throwGlobalError);

export const updateOrganization = (key: string, changes: {}) =>
  post('/api/organizations/update', { key, ...changes });

export const deleteOrganization = (key: string) =>
  post('/api/organizations/delete', { key }).catch(throwGlobalError);

export const searchMembers = (
  data: { organization?: string, p?: number, ps?: number, q?: string, selected?: string }
) => getJSON('/api/organizations/search_members', data);

export const addMember = (data: { login: string, organization: string }) =>
  postJSON('/api/organizations/add_member', data).then(r => r.user);

export const removeMember = (data: { login: string, organization: string }) =>
  post('/api/organizations/remove_member', data);

export const changeProjectVisibility = (organization: string, projectVisibility: string) =>
  post('/api/organizations/update_project_visibility', { organization, projectVisibility });
