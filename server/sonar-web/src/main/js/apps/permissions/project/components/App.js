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
import { without } from 'lodash';
import PageHeader from './PageHeader';
import AllHoldersList from './AllHoldersList';
import * as api from '../../../../api/permissions';
import PageError from '../../shared/components/PageError';
import '../../styles.css';

// TODO helmet

export type Props = {|
  component: {
    configuration?: {
      canApplyPermissionTemplate: boolean
    },
    key: string,
    organization: string,
    qualifier: string
  },
  onRequestFail: Object => void
|};

export type State = {|
  filter: string,
  groups: Array<{
    name: string,
    permissions: Array<string>
  }>,
  loading: boolean,
  query: string,
  selectedPermission?: string,
  users: Array<{
    login: string,
    name: string,
    permissions: Array<string>
  }>
|};

export default class App extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      filter: 'all',
      groups: [],
      loading: true,
      query: '',
      users: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  startLoading = () => {
    if (this.mounted) {
      this.setState({ loading: true });
    }
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  loadHolders = () => {
    if (this.mounted) {
      this.setState({ loading: true });

      const { component } = this.props;
      const { filter, query, selectedPermission } = this.state;

      const getUsers = filter !== 'groups'
        ? api.getPermissionsUsersForComponent(
            component.key,
            query,
            selectedPermission,
            component.organization
          )
        : Promise.resolve([]);

      const getGroups = filter !== 'users'
        ? api.getPermissionsGroupsForComponent(
            component.key,
            query,
            selectedPermission,
            component.organization
          )
        : Promise.resolve([]);

      Promise.all([getUsers, getGroups]).then(
        responses => {
          if (this.mounted) {
            this.setState({ loading: false, groups: responses[1], users: responses[0] });
          }
        },
        error => {
          if (this.mounted) {
            this.props.onRequestFail(error);
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  handleFilterChange = (filter: string) => {
    if (this.mounted) {
      this.setState({ filter }, this.loadHolders);
    }
  };

  handleQueryChange = (query: string) => {
    if (this.mounted) {
      this.setState({ query }, () => {
        if (query.length === 0 || query.length > 2) {
          this.loadHolders();
        }
      });
    }
  };

  handlePermissionSelect = (selectedPermission?: string) => {
    if (this.mounted) {
      this.setState(
        (state: State) => ({
          selectedPermission: state.selectedPermission === selectedPermission
            ? undefined
            : selectedPermission
        }),
        this.loadHolders
      );
    }
  };

  addPermissionToGroup = (group: string, permission: string) =>
    this.state.groups.map(
      candidate =>
        (candidate.name === group
          ? { ...candidate, permissions: [...candidate.permissions, permission] }
          : candidate)
    );

  addPermissionToUser = (user: string, permission: string) =>
    this.state.users.map(
      candidate =>
        (candidate.login === user
          ? { ...candidate, permissions: [...candidate.permissions, permission] }
          : candidate)
    );

  removePermissionFromGroup = (group: string, permission: string) =>
    this.state.groups.map(
      candidate =>
        (candidate.name === group
          ? { ...candidate, permissions: without(candidate.permissions, permission) }
          : candidate)
    );

  removePermissionFromUser = (user: string, permission: string) =>
    this.state.users.map(
      candidate =>
        (candidate.login === user
          ? { ...candidate, permissions: without(candidate.permissions, permission) }
          : candidate)
    );

  grantPermissionToGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState({ loading: true, groups: this.addPermissionToGroup(group, permission) });
      api
        .grantPermissionToGroup(
          this.props.component.key,
          group,
          permission,
          this.props.component.organization
        )
        .then(this.stopLoading, error => {
          if (this.mounted) {
            this.setState({
              loading: false,
              groups: this.removePermissionFromGroup(group, permission)
            });
            this.props.onRequestFail(error);
          }
        });
    }
  };

  grantPermissionToUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState({ loading: true, users: this.addPermissionToUser(user, permission) });
      api
        .grantPermissionToUser(
          this.props.component.key,
          user,
          permission,
          this.props.component.organization
        )
        .then(this.stopLoading, error => {
          if (this.mounted) {
            this.setState({
              loading: false,
              users: this.removePermissionFromUser(user, permission)
            });
            this.props.onRequestFail(error);
          }
        });
    }
  };

  revokePermissionFromGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState({ loading: true, groups: this.removePermissionFromGroup(group, permission) });
      api
        .revokePermissionFromGroup(
          this.props.component.key,
          group,
          permission,
          this.props.component.organization
        )
        .then(this.stopLoading, error => {
          if (this.mounted) {
            this.setState({
              loading: false,
              groups: this.addPermissionToGroup(group, permission)
            });
            this.props.onRequestFail(error);
          }
        });
    }
  };

  revokePermissionFromUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState({ loading: true, users: this.removePermissionFromUser(user, permission) });
      api
        .revokePermissionFromUser(
          this.props.component.key,
          user,
          permission,
          this.props.component.organization
        )
        .then(this.stopLoading, error => {
          if (this.mounted) {
            this.setState({
              loading: false,
              users: this.addPermissionToUser(user, permission)
            });
            this.props.onRequestFail(error);
          }
        });
    }
  };

  render() {
    return (
      <div className="page page-limited">
        <PageHeader
          component={this.props.component}
          loading={this.state.loading}
          loadHolders={this.loadHolders}
        />
        <PageError />
        <AllHoldersList
          component={this.props.component}
          filter={this.state.filter}
          grantPermissionToGroup={this.grantPermissionToGroup}
          grantPermissionToUser={this.grantPermissionToUser}
          groups={this.state.groups}
          onFilterChange={this.handleFilterChange}
          onPermissionSelect={this.handlePermissionSelect}
          onQueryChange={this.handleQueryChange}
          query={this.state.query}
          revokePermissionFromGroup={this.revokePermissionFromGroup}
          revokePermissionFromUser={this.revokePermissionFromUser}
          selectedPermission={this.state.selectedPermission}
          users={this.state.users}
        />
      </div>
    );
  }
}
