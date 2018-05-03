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
import HelpTooltip from '../../../../components/controls/HelpTooltip';
import InstanceMessage from '../../../../components/common/InstanceMessage';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export interface Permission {
  key: string;
  name: string;
  description: string;
}

interface Props {
  onSelectPermission: (permission: string) => void;
  permission: Permission;
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
}

export default class PermissionHeader extends React.PureComponent<Props> {
  handlePermissionClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onSelectPermission(this.props.permission.key);
  };

  renderTooltip = (permission: Permission) => {
    if (this.props.showPublicProjectsWarning && ['user', 'codeviewer'].includes(permission.key)) {
      return (
        <div>
          <InstanceMessage message={permission.description} />
          <div className="alert alert-warning spacer-top">
            {translate('projects_role.public_projects_warning')}
          </div>
        </div>
      );
    }
    return <InstanceMessage message={permission.description} />;
  };

  render() {
    const { permission, selectedPermission } = this.props;
    return (
      <th
        className="permission-column text-center"
        style={{
          backgroundColor: permission.key === selectedPermission ? '#d9edf7' : 'transparent'
        }}>
        <div className="permission-column-inner">
          <Tooltip
            overlay={translateWithParameters(
              'global_permissions.filter_by_x_permission',
              permission.name
            )}>
            <a className="text-middle" href="#" onClick={this.handlePermissionClick}>
              {permission.name}
            </a>
          </Tooltip>
          <HelpTooltip className="spacer-left" overlay={this.renderTooltip(permission)} />
        </div>
      </th>
    );
  }
}
