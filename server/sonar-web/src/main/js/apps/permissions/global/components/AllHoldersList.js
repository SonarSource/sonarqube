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
import React from 'react';
import { connect } from 'react-redux';
import SearchForm from '../../shared/components/SearchForm';
import HoldersList from '../../shared/components/HoldersList';
import {
  loadHolders,
  grantToUser,
  revokeFromUser,
  grantToGroup,
  revokeFromGroup,
  updateFilter,
  updateQuery,
  selectPermission
} from '../store/actions';
import { translate } from '../../../../helpers/l10n';
import {
  getPermissionsAppUsers,
  getPermissionsAppGroups,
  getPermissionsAppQuery,
  getPermissionsAppFilter,
  getPermissionsAppSelectedPermission
} from '../../../../store/rootReducer';

const PERMISSIONS_ORDER = ['admin', 'profileadmin', 'gateadmin', 'scan', 'provisioning'];

const PERMISSIONS_FOR_CUSTOM_ORG = ['admin', 'scan', 'provisioning'];

class AllHoldersList extends React.Component {
  componentDidMount() {
    this.props.loadHolders();
  }

  handleToggleUser(user, permission) {
    const hasPermission = user.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromUser(user.login, permission);
    } else {
      this.props.grantPermissionToUser(user.login, permission);
    }
  }

  handleToggleGroup(group, permission) {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromGroup(group.name, permission);
    } else {
      this.props.grantPermissionToGroup(group.name, permission);
    }
  }

  render() {
    const order = this.props.organization && !this.props.organization.isDefault
      ? PERMISSIONS_FOR_CUSTOM_ORG
      : PERMISSIONS_ORDER;

    const l10nPrefix = this.props.organization ? 'organizations_permissions' : 'global_permissions';

    const permissions = order.map(p => ({
      key: p,
      name: translate(l10nPrefix, p),
      description: translate(l10nPrefix, p, 'desc')
    }));

    return (
      <HoldersList
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        users={this.props.users}
        groups={this.props.groups}
        onSelectPermission={this.props.onSelectPermission}
        onToggleUser={this.handleToggleUser.bind(this)}
        onToggleGroup={this.handleToggleGroup.bind(this)}>

        <SearchForm
          query={this.props.query}
          filter={this.props.filter}
          onSearch={this.props.onSearch}
          onFilter={this.props.onFilter}
        />

      </HoldersList>
    );
  }
}

const mapStateToProps = state => ({
  users: getPermissionsAppUsers(state),
  groups: getPermissionsAppGroups(state),
  query: getPermissionsAppQuery(state),
  filter: getPermissionsAppFilter(state),
  selectedPermission: getPermissionsAppSelectedPermission(state)
});

type OwnProps = {
  organization?: { key: string }
};

const mapDispatchToProps = (dispatch: Function, ownProps: OwnProps) => {
  const organizationKey = ownProps.organization ? ownProps.organization.key : undefined;
  return {
    loadHolders: () => dispatch(loadHolders(organizationKey)),
    onSearch: query => dispatch(updateQuery(query, organizationKey)),
    onFilter: filter => dispatch(updateFilter(filter, organizationKey)),
    onSelectPermission: permission => dispatch(selectPermission(permission, organizationKey)),
    grantPermissionToUser: (login, permission) =>
      dispatch(grantToUser(login, permission, organizationKey)),
    revokePermissionFromUser: (login, permission) =>
      dispatch(revokeFromUser(login, permission, organizationKey)),
    grantPermissionToGroup: (groupName, permission) =>
      dispatch(grantToGroup(groupName, permission, organizationKey)),
    revokePermissionFromGroup: (groupName, permission) =>
      dispatch(revokeFromGroup(groupName, permission, organizationKey))
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(AllHoldersList);
