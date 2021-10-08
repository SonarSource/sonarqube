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
import * as React from 'react';
import { searchUsers } from '../../../api/quality-gates';
import QualityGatePermissionsRenderer from './QualityGatePermissionsRenderer';

interface Props {
  qualityGate: T.QualityGate;
}

interface State {
  loading: boolean;
  users: T.UserBase[];
}

export default class QualityGatePermissions extends React.Component<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
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

  render() {
    const { loading, users } = this.state;
    return <QualityGatePermissionsRenderer loading={loading} users={users} />;
  }
}
