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
import classNames from 'classnames';
import { BareButton, ContentCell, HelperHintIcon } from 'design-system';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import { PermissionDefinition, PermissionDefinitionGroup } from '../../types/types';
import InstanceMessage from '../common/InstanceMessage';
import ClickEventBoundary from '../controls/ClickEventBoundary';
import Tooltip from '../controls/Tooltip';

interface Props {
  onSelectPermission?: (permission: string) => void;
  permission: PermissionDefinition | PermissionDefinitionGroup;
  selectedPermission?: string;
}

export default class PermissionHeader extends React.PureComponent<Props> {
  handlePermissionClick = () => {
    const { permission } = this.props;
    if (this.props.onSelectPermission && !isPermissionDefinitionGroup(permission)) {
      this.props.onSelectPermission(permission.key);
    }
  };

  getTooltipOverlay = () => {
    const { permission } = this.props;

    if (isPermissionDefinitionGroup(permission)) {
      return permission.permissions.map((permission) => (
        <React.Fragment key={permission.key}>
          <b className="sw-mr-1">{permission.name}:</b>
          <InstanceMessage key={permission.key} message={permission.description} />
          <br />
        </React.Fragment>
      ));
    }

    return <InstanceMessage message={permission.description} />;
  };

  render() {
    const { onSelectPermission, permission } = this.props;
    let name;
    if (isPermissionDefinitionGroup(permission)) {
      name = translate('global_permissions', permission.category);
    } else {
      name = onSelectPermission ? (
        <ClickEventBoundary>
          <BareButton onClick={this.handlePermissionClick}>
            <Tooltip
              overlay={translateWithParameters(
                'global_permissions.filter_by_x_permission',
                permission.name,
              )}
            >
              <span>{permission.name}</span>
            </Tooltip>
          </BareButton>
        </ClickEventBoundary>
      ) : (
        permission.name
      );
    }
    return (
      <ContentCell
        scope="col"
        className={classNames('sw-justify-center', {
          selected:
            !isPermissionDefinitionGroup(permission) &&
            permission.key === this.props.selectedPermission,
        })}
      >
        <div className="sw-flex sw-content-center">
          <div className="sw-grow-1 sw-text-center">{name}</div>

          <HelpTooltip className="sw-ml-2" overlay={this.getTooltipOverlay()}>
            <HelperHintIcon aria-label="help-tooltip" />
          </HelpTooltip>
        </div>
      </ContentCell>
    );
  }
}
