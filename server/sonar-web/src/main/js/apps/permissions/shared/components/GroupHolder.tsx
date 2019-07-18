/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import GroupIcon from 'sonar-ui-common/components/icons/GroupIcon';
import { isPermissionDefinitionGroup } from '../../utils';
import PermissionCell from './PermissionCell';

interface Props {
  group: T.PermissionGroup;
  onToggle: (group: T.PermissionGroup, permission: string) => Promise<void>;
  permissions: T.PermissionDefinitions;
  selectedPermission?: string;
}

interface State {
  loading: string[];
}

export default class GroupHolder extends React.PureComponent<Props, State> {
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
        .onToggle(this.props.group, permission)
        .then(() => this.stopLoading(permission), () => this.stopLoading(permission));
    }
  };

  render() {
    const { group } = this.props;

    return (
      <tr>
        <td className="nowrap text-middle">
          <div className="display-inline-block text-middle big-spacer-right">
            <GroupIcon />
          </div>
          <div className="display-inline-block text-middle">
            <div>
              <strong>{group.name}</strong>
            </div>
            <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
              {group.description}
            </div>
          </div>
        </td>
        {this.props.permissions.map(permission => (
          <PermissionCell
            key={isPermissionDefinitionGroup(permission) ? permission.category : permission.key}
            loading={this.state.loading}
            onCheck={this.handleCheck}
            permission={permission}
            permissionItem={group}
            selectedPermission={this.props.selectedPermission}
          />
        ))}
      </tr>
    );
  }
}
