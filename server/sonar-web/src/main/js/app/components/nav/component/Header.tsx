/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import Favorite from '../../../../components/controls/Favorite';
import { ProjectAlmBindingResponse } from '../../../../types/alm-settings';
import { BranchLike } from '../../../../types/branch-like';
import { Component, Organization } from '../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import BranchLikeNavigation from './branch-like/BranchLikeNavigation';
import CurrentBranchLikeMergeInformation from './branch-like/CurrentBranchLikeMergeInformation';
import { Breadcrumb } from './Breadcrumb';
import OrganizationAvatar from "../../../../apps/organizations/components/OrganizationAvatar";
import OrganizationLink from "../../../../apps/organizations/components/OrganizationLink";

export interface HeaderProps {
  branchLikes: BranchLike[];
  component: Component;
  currentBranchLike: BranchLike | undefined;
  currentUser: CurrentUser;
  projectBinding?: ProjectAlmBindingResponse;
  organization: Organization;
}

export function Header(props: HeaderProps) {
  const { branchLikes, component, currentBranchLike, currentUser, projectBinding, organization } = props;

  return (
    <>
      <Helmet title={component.name} />
      <div className="display-flex-center flex-shrink">
        {organization &&
            <>
              <OrganizationAvatar organization={organization} />
              <OrganizationLink
                  className="navbar-context-header-breadcrumb-link link-base-color link-no-underline spacer-left"
                  organization={organization}>
                {organization.name}
              </OrganizationLink>
              <span className="slash-separator" />
            </>
        }
        <Breadcrumb component={component} currentBranchLike={currentBranchLike} />
        {isLoggedIn(currentUser) && (
          <Favorite
            className="spacer-left"
            component={component.key}
            favorite={Boolean(component.isFavorite)}
            qualifier={component.qualifier}
          />
        )}
        {currentBranchLike && (
          <>
            <BranchLikeNavigation
              branchLikes={branchLikes}
              component={component}
              currentBranchLike={currentBranchLike}
              projectBinding={projectBinding}
            />
            <CurrentBranchLikeMergeInformation currentBranchLike={currentBranchLike} />
          </>
        )}
      </div>
    </>
  );
}

export default withCurrentUserContext(React.memo(Header));
