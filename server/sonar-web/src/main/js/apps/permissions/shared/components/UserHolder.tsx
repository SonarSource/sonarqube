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
import { without } from 'lodash';
import * as React from 'react';
import Avatar from '../../../../components/ui/Avatar';
import { translate } from '../../../../helpers/l10n';
import { PermissionDefinitions, PermissionUser } from '../../../../types/types';
import { isPermissionDefinitionGroup } from '../../utils';
import PermissionCell from './PermissionCell';

interface Props {
  onToggle: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  selectedPermission?: string;
  user: PermissionUser;
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
      this.setState((state) => ({ loading: without(state.loading, permission) }));
    }
  };

  handleCheck = (_checked: boolean, permission?: string) => {
    if (permission !== undefined) {
      this.setState((state) => ({ loading: [...state.loading, permission] }));
      this.props.onToggle(this.props.user, permission).then(
        () => this.stopLoading(permission),
        () => this.stopLoading(permission)
      );
    }
  };

  render() {
    const { user } = this.props;
    const permissionCells = this.props.permissions.map((permission) => (
      <PermissionCell
        key={isPermissionDefinitionGroup(permission) ? permission.category : permission.key}
        loading={this.state.loading}
        onCheck={this.handleCheck}
        permission={permission}
        permissionItem={user}
        selectedPermission={this.props.selectedPermission}
      />
    ));

    if (user.login === '<creator>') {
      return (
        <tr>
          <td className="nowrap text-middle">
            <div>
              <strong>{user.name}</strong>
            </div>
            <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
              {translate('permission_templates.project_creators.explanation')}
            </div>
          </td>
          {permissionCells}
        </tr>
      );
    }

    return (
      <tr>
        <td className="nowrap text-middle">
          <div className="display-flex-center">
            <Avatar
              className="text-middle big-spacer-right flex-0"
              hash={user.avatar}
              name={user.name}
              size={36}
            />
            <div className="max-width-100">
              <div className="max-width-100 text-ellipsis">
                <strong>{user.name}</strong>
                <span className="note spacer-left">{user.login}</span>
              </div>
              <div className="little-spacer-top max-width-100 text-ellipsis">{user.email}</div>
            </div>
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }
}
