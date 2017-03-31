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
  updateQuery,
  updateFilter,
  selectPermission
} from '../store/actions';
import { translate } from '../../../../helpers/l10n';
import { PERMISSIONS_ORDER_BY_QUALIFIER } from '../constants';
import {
  getPermissionsAppUsers,
  getPermissionsAppGroups,
  getPermissionsAppQuery,
  getPermissionsAppFilter,
  getPermissionsAppSelectedPermission
} from '../../../../store/rootReducer';

class AllHoldersList extends React.Component {
  static propTypes = {
    project: React.PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.loadHolders(this.props.project.key);
  }

  handleSearch(query) {
    this.props.onSearch(this.props.project.key, query);
  }

  handleFilter(filter) {
    this.props.onFilter(this.props.project.key, filter);
  }

  handleToggleUser(user, permission) {
    const hasPermission = user.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromUser(this.props.project.key, user.login, permission);
    } else {
      this.props.grantPermissionToUser(this.props.project.key, user.login, permission);
    }
  }

  handleToggleGroup(group, permission) {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromGroup(this.props.project.key, group.name, permission);
    } else {
      this.props.grantPermissionToGroup(this.props.project.key, group.name, permission);
    }
  }

  handleSelectPermission(permission) {
    this.props.onSelectPermission(this.props.project.key, permission);
  }

  render() {
    const order = PERMISSIONS_ORDER_BY_QUALIFIER[this.props.project.qualifier];
    const permissions = order.map(p => ({
      key: p,
      name: translate('projects_role', p),
      description: translate('projects_role', p, 'desc')
    }));

    return (
      <HoldersList
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        users={this.props.users}
        groups={this.props.groups}
        onSelectPermission={this.handleSelectPermission.bind(this)}
        onToggleUser={this.handleToggleUser.bind(this)}
        onToggleGroup={this.handleToggleGroup.bind(this)}>

        <SearchForm
          query={this.props.query}
          filter={this.props.filter}
          onSearch={this.handleSearch.bind(this)}
          onFilter={this.handleFilter.bind(this)}
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
  project: {
    organization?: string
  }
};

const mapDispatchToProps = (dispatch: Function, ownProps: OwnProps) => ({
  loadHolders: projectKey => dispatch(loadHolders(projectKey, ownProps.project.organization)),
  onSearch: (projectKey, query) =>
    dispatch(updateQuery(projectKey, query, ownProps.project.organization)),
  onFilter: (projectKey, filter) =>
    dispatch(updateFilter(projectKey, filter, ownProps.project.organization)),
  onSelectPermission: (projectKey, permission) =>
    dispatch(selectPermission(projectKey, permission, ownProps.project.organization)),
  grantPermissionToUser: (projectKey, login, permission) =>
    dispatch(grantToUser(projectKey, login, permission, ownProps.project.organization)),
  revokePermissionFromUser: (projectKey, login, permission) =>
    dispatch(revokeFromUser(projectKey, login, permission, ownProps.project.organization)),
  grantPermissionToGroup: (projectKey, groupName, permission) =>
    dispatch(grantToGroup(projectKey, groupName, permission, ownProps.project.organization)),
  revokePermissionFromGroup: (projectKey, groupName, permission) =>
    dispatch(revokeFromGroup(projectKey, groupName, permission, ownProps.project.organization))
});

export default connect(mapStateToProps, mapDispatchToProps)(AllHoldersList);
