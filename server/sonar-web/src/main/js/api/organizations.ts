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
import { getJSON } from 'sonar-ui-common/helpers/request';
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
