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
import { Link } from 'design-system';
import * as React from 'react';
import OrganizationAvatar from '../../../../apps/organizations/components/OrganizationAvatar';
import { Component, Organization } from '../../../../types/types';
import { CurrentUser } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import { Breadcrumb } from './Breadcrumb';
import BranchLikeNavigation from './branch-like/BranchLikeNavigation';

export interface HeaderProps {
  component: Component;
  currentUser: CurrentUser;
  organization: Organization;
  comparisonBranchesEnabled: boolean;
}

export function Header(props: HeaderProps) {
  const { component, currentUser, organization, comparisonBranchesEnabled } = props;

  return (
    <div className="sw-flex sw-flex-shrink sw-items-center">
      {organization && (
        <>
          <OrganizationAvatar organization={organization} />
          <Link
            className="sw-ml-2 navbar-context-header-breadcrumb-link link-base-color link-no-underline spacer-left"
            to={`/organizations/${organization.kee}`}
          >
            {organization.name}
          </Link>
          <span className="slash-separator" />
        </>
      )}

      <Breadcrumb component={component} currentUser={currentUser} />

      <BranchLikeNavigation
        component={component}
        comparisonBranchesEnabled={comparisonBranchesEnabled}
      />
    </div>
  );
}

export default withCurrentUserContext(React.memo(Header));
