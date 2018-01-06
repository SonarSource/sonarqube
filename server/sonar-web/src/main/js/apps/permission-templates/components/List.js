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
import ListHeader from './ListHeader';
import ListItem from './ListItem';
import { PermissionTemplateType, CallbackType } from '../propTypes';

export default class List extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    permissionTemplates: PropTypes.arrayOf(PermissionTemplateType).isRequired,
    permissions: PropTypes.array.isRequired,
    topQualifiers: PropTypes.array.isRequired,
    refresh: CallbackType
  };

  render() {
    const permissionTemplates = this.props.permissionTemplates.map(p => (
      <ListItem
        key={p.id}
        organization={this.props.organization}
        permissionTemplate={p}
        topQualifiers={this.props.topQualifiers}
        refresh={this.props.refresh}
      />
    ));

    return (
      <div className="boxed-group boxed-group-inner">
        <table id="permission-templates" className="data zebra permissions-table">
          <ListHeader organization={this.props.organization} permissions={this.props.permissions} />
          <tbody>{permissionTemplates}</tbody>
        </table>
      </div>
    );
  }
}
