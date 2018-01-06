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
import React from 'react';
import PropTypes from 'prop-types';
import NameCell from './NameCell';
import PermissionCell from './PermissionCell';
import ActionsCell from './ActionsCell';
import { PermissionTemplateType, CallbackType } from '../propTypes';

export default class ListItem extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    permissionTemplate: PermissionTemplateType.isRequired,
    topQualifiers: PropTypes.array.isRequired,
    refresh: CallbackType
  };

  render() {
    const permissions = this.props.permissionTemplate.permissions.map(p => (
      <PermissionCell key={p.key} permission={p} />
    ));

    return (
      <tr data-id={this.props.permissionTemplate.id} data-name={this.props.permissionTemplate.name}>
        <NameCell
          organization={this.props.organization}
          permissionTemplate={this.props.permissionTemplate}
          topQualifiers={this.props.topQualifiers}
        />

        {permissions}

        <td className="nowrap thin text-right">
          <ActionsCell
            organization={this.props.organization}
            permissionTemplate={this.props.permissionTemplate}
            topQualifiers={this.props.topQualifiers}
            refresh={this.props.refresh}
          />
        </td>
      </tr>
    );
  }
}
