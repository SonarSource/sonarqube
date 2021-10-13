/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { addUser, removeUser, searchUsers } from '../../../api/quality-gates';
import QualityGatePermissionsRenderer from './QualityGatePermissionsRenderer';

interface Props {
  qualityGate: T.QualityGate;
}

interface State {
  submitting: boolean;
  loading: boolean;
  showAddModal: boolean;
  userPermissionToDelete?: T.UserBase;
  users: T.UserBase[];
}

export default class QualityGatePermissions extends React.Component<Props, State> {
  mounted = false;
  state: State = {
    submitting: false,
    loading: true,
    showAddModal: false,
    users: []
  };

  componentDidMount() {
    this.mounted = true;

    this.fetchPermissions();
  }

  componentDidUpdate(newProps: Props) {
    if (this.props.qualityGate.id !== newProps.qualityGate.id) {
      this.fetchPermissions();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPermissions = async () => {
    const { qualityGate } = this.props;
    this.setState({ loading: true });

    const { users } = await searchUsers({
      qualityGate: qualityGate.id,
      selected: 'selected'
    }).catch(() => ({
      users: []
    }));

    if (this.mounted) {
      this.setState({ loading: false, users });
    }
  };

  handleCloseAddPermission = () => {
    this.setState({ showAddModal: false });
  };

  handleClickAddPermission = () => {
    this.setState({ showAddModal: true });
  };

  handleSubmitAddPermission = async (user: T.UserBase) => {
    const { qualityGate } = this.props;
    this.setState({ submitting: true });

    let error = false;
    try {
      await addUser({ qualityGate: qualityGate.id, userLogin: user.login });
    } catch {
      error = true;
    }

    if (this.mounted) {
      this.setState(({ users }) => {
        return {
          submitting: false,
          showAddModal: error,
          users: sortBy(users.concat(user), u => u.name)
        };
      });
    }
  };

  handleCloseDeletePermission = () => {
    this.setState({ userPermissionToDelete: undefined });
  };

  handleClickDeletePermission = (userPermissionToDelete?: T.UserBase) => {
    this.setState({ userPermissionToDelete });
  };

  handleConfirmDeletePermission = async (user: T.UserBase) => {
    const { qualityGate } = this.props;

    let error = false;
    try {
      await removeUser({ qualityGate: qualityGate.id, userLogin: user.login });
    } catch {
      error = true;
    }

    if (this.mounted && !error) {
      this.setState(({ users }) => ({
        users: users.filter(u => u.login !== user.login)
      }));
    }
  };

  render() {
    const { qualityGate } = this.props;
    const { submitting, loading, showAddModal, userPermissionToDelete, users } = this.state;
    return (
      <QualityGatePermissionsRenderer
        loading={loading}
        onClickAddPermission={this.handleClickAddPermission}
        onCloseAddPermission={this.handleCloseAddPermission}
        onSubmitAddPermission={this.handleSubmitAddPermission}
        onCloseDeletePermission={this.handleCloseDeletePermission}
        onClickDeletePermission={this.handleClickDeletePermission}
        onConfirmDeletePermission={this.handleConfirmDeletePermission}
        qualityGate={qualityGate}
        showAddModal={showAddModal}
        submitting={submitting}
        userPermissionToDelete={userPermissionToDelete}
        users={users}
      />
    );
  }
}
