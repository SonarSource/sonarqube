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

import { sortBy } from 'lodash';
import * as React from 'react';
import {
  addGroup,
  addUser,
  removeGroup,
  removeUser,
  searchGroups,
  searchUsers,
} from '../../../api/quality-gates';
import { Group, SearchPermissionsParameters, isUser } from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import { UserBase } from '../../../types/users';
import QualityGatePermissionsRenderer from './QualityGatePermissionsRenderer';

interface Props {
  qualityGate: QualityGate;
  organization: string;
}

interface State {
  groups: Group[];
  loading: boolean;
  permissionToDelete?: UserBase | Group;
  showAddModal: boolean;
  submitting: boolean;
  users: UserBase[];
}

export default class QualityGatePermissions extends React.Component<Props, State> {
  mounted = false;
  state: State = {
    groups: [],
    submitting: false,
    loading: true,
    showAddModal: false,
    users: [],
  };

  componentDidMount() {
    this.mounted = true;

    this.fetchPermissions();
  }

  componentDidUpdate(newProps: Props) {
    if (this.props.qualityGate.name !== newProps.qualityGate.name) {
      this.fetchPermissions();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPermissions = async () => {
    const { qualityGate, organization } = this.props;
    this.setState({ loading: true });

    const params: SearchPermissionsParameters = {
      organization,
      gateName: qualityGate.name,
      selected: 'selected',
    };
    const [{ users }, { groups }] = await Promise.all([
      searchUsers(params).catch(() => ({ users: [] })),
      searchGroups(params).catch(() => ({ groups: [] })),
    ]);

    if (this.mounted) {
      this.setState({ groups, loading: false, users });
    }
  };

  handleCloseAddPermission = () => {
    this.setState({ showAddModal: false });
  };

  handleClickAddPermission = () => {
    this.setState({ showAddModal: true });
  };

  handleSubmitAddPermission = async (item: UserBase | Group) => {
    const { organization, qualityGate } = this.props;
    this.setState({ submitting: true });

    let error = false;
    try {
      if (isUser(item)) {
        await addUser({ organization, gateName: qualityGate.name, login: item.login });
      } else {
        await addGroup({ organization, gateName: qualityGate.name, groupName: item.name });
      }
    } catch {
      error = true;
    }

    if (this.mounted && !error) {
      if (isUser(item)) {
        this.setState(({ users }) => ({
          showAddModal: false,
          users: sortBy(users.concat(item), (u) => u.name),
        }));
      } else {
        this.setState(({ groups }) => ({
          showAddModal: false,
          groups: sortBy(groups.concat(item), (g) => g.name),
        }));
      }
    }

    if (this.mounted) {
      this.setState({
        submitting: false,
      });
    }
  };

  handleCloseDeletePermission = () => {
    this.setState({ permissionToDelete: undefined });
  };

  handleClickDeletePermission = (permissionToDelete?: UserBase | Group) => {
    this.setState({ permissionToDelete });
  };

  handleConfirmDeletePermission = async (item: UserBase | Group) => {
    const { organization, qualityGate } = this.props;

    let error = false;
    try {
      if (isUser(item)) {
        await removeUser({ organization, gateName: qualityGate.name, login: item.login });
      } else {
        await removeGroup({ organization, gateName: qualityGate.name, groupName: item.name });
      }
    } catch {
      error = true;
    }

    if (this.mounted && !error) {
      if (isUser(item)) {
        this.setState(({ users }) => ({
          users: users.filter((u) => u.login !== item.login),
          permissionToDelete: undefined,
        }));
      } else {
        this.setState(({ groups }) => ({
          groups: groups.filter((g) => g.name !== item.name),
          permissionToDelete: undefined,
        }));
      }
    }
  };

  render() {
    const { qualityGate } = this.props;
    const { groups, submitting, loading, showAddModal, permissionToDelete, users } = this.state;
    return (
      <QualityGatePermissionsRenderer
        organization={this.props.organization}
        groups={groups}
        loading={loading}
        onClickAddPermission={this.handleClickAddPermission}
        onCloseAddPermission={this.handleCloseAddPermission}
        onSubmitAddPermission={this.handleSubmitAddPermission}
        onCloseDeletePermission={this.handleCloseDeletePermission}
        onClickDeletePermission={this.handleClickDeletePermission}
        onConfirmDeletePermission={this.handleConfirmDeletePermission}
        permissionToDelete={permissionToDelete}
        qualityGate={qualityGate}
        showAddModal={showAddModal}
        submitting={submitting}
        users={users}
      />
    );
  }
}
