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
import * as classNames from 'classnames';
import * as React from 'react';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import InstanceMessage from '../../../../components/common/InstanceMessage';
import { isPermissionDefinitionGroup } from '../../utils';

interface Props {
  onSelectPermission?: (permission: string) => void;
  permission: T.PermissionDefinition | T.PermissionDefinitionGroup;
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
}

export default class PermissionHeader extends React.PureComponent<Props> {
  handlePermissionClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const { permission } = this.props;
    if (this.props.onSelectPermission && !isPermissionDefinitionGroup(permission)) {
      this.props.onSelectPermission(permission.key);
    }
  };

  getTooltipOverlay = () => {
    const { permission } = this.props;

    if (isPermissionDefinitionGroup(permission)) {
      return permission.permissions.map(permission => (
        <React.Fragment key={permission.key}>
          <b className="little-spacer-right">{permission.name}:</b>
          <InstanceMessage key={permission.key} message={permission.description} />
          <br />
        </React.Fragment>
      ));
    } else {
      if (this.props.showPublicProjectsWarning && ['user', 'codeviewer'].includes(permission.key)) {
        return (
          <div>
            <InstanceMessage message={permission.description} />
            <Alert className="spacer-top" variant="warning">
              {translate('projects_role.public_projects_warning')}
            </Alert>
          </div>
        );
      }
      return <InstanceMessage message={permission.description} />;
    }
  };

  render() {
    const { onSelectPermission, permission } = this.props;
    let name;
    if (isPermissionDefinitionGroup(permission)) {
      name = translate('global_permissions', permission.category);
    } else {
      name = onSelectPermission ? (
        <Tooltip
          overlay={translateWithParameters(
            'global_permissions.filter_by_x_permission',
            permission.name
          )}>
          <a href="#" onClick={this.handlePermissionClick}>
            {permission.name}
          </a>
        </Tooltip>
      ) : (
        permission.name
      );
    }
    return (
      <th
        className={classNames('permission-column text-center text-middle', {
          selected:
            !isPermissionDefinitionGroup(permission) &&
            permission.key === this.props.selectedPermission
        })}>
        <div className="permission-column-inner">
          {name}
          <HelpTooltip className="spacer-left" overlay={this.getTooltipOverlay()} />
        </div>
      </th>
    );
  }
}
