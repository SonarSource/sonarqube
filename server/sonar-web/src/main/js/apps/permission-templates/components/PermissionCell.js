/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { PermissionType, CallbackType } from '../propTypes';
import { translate } from '../../../helpers/l10n';

export default class PermissionCell extends React.Component {
  static propTypes = {
    permission: PermissionType.isRequired,
    onShowUsers: CallbackType,
    onShowGroups: CallbackType
  };

  handleShowUsers (e) {
    e.preventDefault();
    this.props.onShowUsers(this.props.permission);
  }

  handleShowGroups (e) {
    e.preventDefault();
    this.props.onShowGroups(this.props.permission);
  }

  render () {
    const { permission: p } = this.props;

    return (
        <td
            className="permission-column"
            data-permission={p.key}>
          <table>
            <tbody>
              <tr>
                <td className="spacer-right">
                  Users
                </td>
                <td className="spacer-left bordered-left">
                  {p.usersCount}
                </td>
                <td className="spacer-left">
                  <a
                      onClick={this.handleShowUsers.bind(this)}
                      className="icon-bullet-list js-update-users"
                      title="Update Users"
                      data-toggle="tooltip"
                      href="#"/>
                </td>
              </tr>
              <tr>
                <td className="spacer-right">
                  Groups
                </td>
                <td className="spacer-left bordered-left">
                  {p.groupsCount}
                </td>
                <td className="spacer-left">
                  <a
                      onClick={this.handleShowGroups.bind(this)}
                      className="icon-bullet-list js-update-groups"
                      title="Update Users"
                      data-toggle="tooltip"
                      href="#"/>
                </td>
              </tr>
            </tbody>
          </table>

          {p.withProjectCreator && (
              <div className="spacer-top">
                <span className="badge badge-focus js-project-creators"
                      title={translate('permission_templates.project_creators.explanation')}
                      data-toggle="tooltip">
                  {translate('permission_templates.project_creators')}
                </span>
              </div>
          )}
        </td>
    );
  }
}
