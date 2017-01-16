/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { getJSON, post } from '../helpers/request';

export const getOrganizations = (organizations?: Array<string>) => {
  const data = {};
  if (organizations) {
    Object.assign(data, { organizations: organizations.join() });
  }
  return getJSON('/api/organizations/search', data);
};

type GetOrganizationType = null | {
  avatar: null | string,
  description: null | string,
  key: string,
  name: string,
  url: null | string
};

type GetOrganizationNavigation = {
  canAdmin: boolean,
  isDefault: boolean
};

export const getOrganization = (key: string): Promise<GetOrganizationType> => {
  return getOrganizations([key]).then(r => r.organizations.find(o => o.key === key));
};

export const getOrganizationNavigation = (key: string): Promise<GetOrganizationNavigation> => {
  return getJSON('/api/navigation/organization', { organization: key }).then(r => r.organization);
};

export const updateOrganization = (key: string, changes: {}) => (
    post('/api/organizations/update', { key, ...changes })
);
