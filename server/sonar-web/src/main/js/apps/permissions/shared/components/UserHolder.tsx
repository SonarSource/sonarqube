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
import { without } from 'lodash';
import Avatar from '../../../../components/ui/Avatar';
import Checkbox from '../../../../components/controls/Checkbox';
import { PermissionUser } from '../../../../api/permissions';
import { translate } from '../../../../helpers/l10n';

interface Props {
  user: PermissionUser;
  permissions: string[];
  selectedPermission?: string;
  permissionsOrder: string[];
  onToggle: (user: PermissionUser, permission: string) => Promise<void>;
}

interface State {
  loading: string[];
}

export default class UserHolder extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: [] };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = (permission: string) => {
    if (this.mounted) {
      this.setState(state => ({ loading: without(state.loading, permission) }));
    }
  };

  handleCheck = (_checked: boolean, permission?: string) => {
    if (permission !== undefined) {
      this.setState(state => ({ loading: [...state.loading, permission] }));
      this.props
        .onToggle(this.props.user, permission)
        .then(() => this.stopLoading(permission), () => this.stopLoading(permission));
    }
  };

  render() {
    const { selectedPermission } = this.props;
    const permissionCells = this.props.permissionsOrder.map(permission => (
      <td
        className="text-center text-middle"
        key={permission}
        style={{ backgroundColor: permission === selectedPermission ? '#d9edf7' : 'transparent' }}>
        <Checkbox
          checked={this.props.permissions.includes(permission)}
          disabled={this.state.loading.includes(permission)}
          id={permission}
          onCheck={this.handleCheck}
        />
      </td>
    ));

    const { user } = this.props;
    if (user.login === '<creator>') {
      return (
        <tr>
          <td className="nowrap">
            <div className="display-inline-block text-middle">
              <div>
                <strong>{user.name}</strong>
              </div>
              <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
                {translate('permission_templates.project_creators.explanation')}
              </div>
            </div>
          </td>
          {permissionCells}
        </tr>
      );
    }

    return (
      <tr>
        <td className="nowrap">
          <Avatar
            className="text-middle big-spacer-right"
            hash={user.avatar}
            name={user.name}
            size={36}
          />
          <div className="display-inline-block text-middle">
            <div>
              <strong>{user.name}</strong>
              <span className="note spacer-left">{user.login}</span>
            </div>
            <div className="little-spacer-top">{user.email}</div>
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }
}
