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
import * as React from 'react';
import ContextNavBar from 'sonar-ui-common/components/ui/ContextNavBar';
import { rawSizes } from '../../../app/theme';
import OrganizationNavigationHeader from './OrganizationNavigationHeader';
import OrganizationNavigationMenuContainer from './OrganizationNavigationMenuContainer';
import OrganizationNavigationMeta from './OrganizationNavigationMeta';

interface Props {
  currentUser: T.CurrentUser;
  location: { pathname: string };
  organization: T.Organization;
  userOrganizations: T.Organization[];
}

export default function OrganizationNavigation({
  currentUser,
  location,
  organization,
  userOrganizations
}: Props) {
  return (
    <ContextNavBar height={rawSizes.contextNavHeightRaw} id="context-navigation">
      <div className="navbar-context-justified">
        <OrganizationNavigationHeader
          currentUser={currentUser}
          organization={organization}
          organizations={userOrganizations}
        />
        <OrganizationNavigationMeta
          currentUser={currentUser}
          organization={organization}
          userOrganizations={userOrganizations}
        />
      </div>
      <OrganizationNavigationMenuContainer location={location} organization={organization} />
    </ContextNavBar>
  );
}
