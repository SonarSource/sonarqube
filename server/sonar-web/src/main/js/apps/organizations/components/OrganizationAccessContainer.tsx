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
import * as React from 'react';
import { connect } from 'react-redux';
import { RouterState } from 'react-router';
import { getOrganizationByKey, getCurrentUser } from '../../../store/rootReducer';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { Organization, CurrentUser, isLoggedIn } from '../../../app/types';
import { isCurrentUserMemberOf, hasPrivateAccess } from '../../../helpers/organizations';

interface StateToProps {
  currentUser: CurrentUser;
  organization?: Organization;
}

interface OwnProps extends RouterState {
  children: JSX.Element;
}

interface Props extends StateToProps, Pick<OwnProps, 'children' | 'location'> {
  hasAccess: (props: Props) => boolean;
}

export class OrganizationAccess extends React.PureComponent<Props> {
  componentDidMount() {
    this.checkPermissions();
  }

  componentDidUpdate() {
    this.checkPermissions();
  }

  checkPermissions = () => {
    if (!this.props.hasAccess(this.props)) {
      handleRequiredAuthorization();
    }
  };

  render() {
    if (!this.props.hasAccess(this.props)) {
      return null;
    }
    return React.cloneElement(this.props.children, {
      location: this.props.location,
      organization: this.props.organization
    });
  }
}

const mapStateToProps = (state: any, ownProps: OwnProps) => ({
  currentUser: getCurrentUser(state),
  organization: getOrganizationByKey(state, ownProps.params.organizationKey)
});

const OrganizationAccessContainer = connect<StateToProps, {}, OwnProps>(mapStateToProps)(
  OrganizationAccess
);

export function OrganizationPrivateAccess(props: OwnProps) {
  return (
    <OrganizationAccessContainer
      hasAccess={({ organization }: StateToProps) => hasPrivateAccess(organization)}
      {...props}
    />
  );
}

export function OrganizationMembersAccess(props: OwnProps) {
  return (
    <OrganizationAccessContainer
      hasAccess={({ organization }: StateToProps) => isCurrentUserMemberOf(organization)}
      {...props}
    />
  );
}

export function hasAdminAccess({
  currentUser,
  organization
}: Pick<StateToProps, 'currentUser' | 'organization'>) {
  const isAdmin = isLoggedIn(currentUser) && organization && organization.canAdmin;
  return Boolean(isAdmin);
}

export function OrganizationAdminAccess(props: OwnProps) {
  return <OrganizationAccessContainer hasAccess={hasAdminAccess} {...props} />;
}
