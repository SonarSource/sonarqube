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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { connect } from 'react-redux';
import Favorite from '../../../../components/controls/Favorite';
import { isLoggedIn } from '../../../../helpers/users';
import { getCurrentUser, getOrganizationByKey, Store } from '../../../../store/rootReducer';
import { BranchLike } from '../../../../types/branch-like';
import BranchLikeNavigation from './branch-like/BranchLikeNavigation';
import CurrentBranchLikeMergeInformation from './branch-like/CurrentBranchLikeMergeInformation';
import { Breadcrumb } from './Breadcrumb';
import { isSonarCloud } from "../../../../helpers/system";
import OrganizationAvatar from "../../../../components/common/OrganizationAvatar";
import OrganizationLink from "../../../../components/ui/OrganizationLink";

export interface HeaderProps {
  branchLikes: BranchLike[];
  component: T.Component;
  comparisonBranchesEnabled: boolean;
  currentBranchLike: BranchLike | undefined;
  currentUser: T.CurrentUser;
  organization: T.Organization;
}

export function Header(props: HeaderProps) {
  const { branchLikes, component, currentBranchLike, currentUser, organization } = props;

  return (
    <>
      <Helmet title={component.name} />
      <header className="display-flex-center flex-shrink">
        {organization &&
          isSonarCloud() && (
            <>
              <OrganizationAvatar organization={organization} />
              <OrganizationLink
                  className="navbar-context-header-breadcrumb-link link-base-color link-no-underline spacer-left"
                  organization={organization}>
                {organization.name}
              </OrganizationLink>
              <span className="slash-separator" />
            </>
        )}
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
              comparisonBranchesEnabled={props.comparisonBranchesEnabled}
              currentBranchLike={currentBranchLike}
            />
            <CurrentBranchLikeMergeInformation currentBranchLike={currentBranchLike} />
          </>
        )}
      </header>
    </>
  );
}

const mapStateToProps = (state: Store, ownProps: HeaderProps) => ({
  organization: getOrganizationByKey(state, ownProps.component.organization),
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(React.memo(Header));
