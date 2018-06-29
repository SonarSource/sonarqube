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

import getStore from '../app/utils/getStore';
import { Organization, isLoggedIn, OrganizationSubscription } from '../app/types';
import { getCurrentUser, getMyOrganizations, getOrganizationByKey } from '../store/rootReducer';

function getRealOrganization(
  organization?: Organization | string,
  state?: any
): Organization | undefined {
  if (typeof organization === 'string') {
    state = state || getStore().getState();
    return getOrganizationByKey(state, organization);
  }

  return organization;
}

function isPaidOrganization(organization: Organization | undefined): boolean {
  return Boolean(organization && organization.subscription === OrganizationSubscription.Paid);
}

export function hasPrivateAccess(organization: Organization | string | undefined): boolean {
  const realOrg = getRealOrganization(organization);
  return !isPaidOrganization(realOrg) || isCurrentUserMemberOf(realOrg);
}

export function isCurrentUserMemberOf(organization: Organization | string | undefined): boolean {
  const state = getStore().getState();
  const currentUser = getCurrentUser(state);
  const userOrganizations = getMyOrganizations(state);
  const realOrg = getRealOrganization(organization, state);
  return Boolean(
    realOrg &&
      isLoggedIn(currentUser) &&
      (realOrg.canAdmin || userOrganizations.some(org => org.key === realOrg.key))
  );
}
