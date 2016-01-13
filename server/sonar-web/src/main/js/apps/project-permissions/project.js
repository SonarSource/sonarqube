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
import UsersView from './users-view';
import GroupsView from './groups-view';
import ApplyTemplateView from './apply-template-view';
import { getComponentUrl } from '../../helpers/urls';
import QualifierIcon from '../../components/shared/qualifier-icon';

export default React.createClass({
  propTypes: {
    project: React.PropTypes.object.isRequired,
    permissionTemplates: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  showGroups(permission, e) {
    e.preventDefault();
    new GroupsView({
      permission: permission,
      project: this.props.project.id,
      projectName: this.props.project.name,
      refresh: this.props.refresh
    }).render();
  },

  showUsers(permission, e) {
    e.preventDefault();
    new UsersView({
      permission: permission,
      project: this.props.project.id,
      projectName: this.props.project.name,
      refresh: this.props.refresh
    }).render();
  },

  applyTemplate(e) {
    e.preventDefault();
    new ApplyTemplateView({
      permissionTemplates: this.props.permissionTemplates,
      project: this.props.project,
      refresh: this.props.refresh
    }).render();
  },

  render() {
    let permissions = this.props.project.permissions.map(p => {
      return (
          <td key={p.key}>
            <table>
              <tbody>
              <tr>
                <td className="spacer-right">Users</td>
                <td className="spacer-left bordered-left">{p.usersCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showUsers.bind(this, p.key)} className="icon-bullet-list" title="Update Users"
                     data-toggle="tooltip" href="#"></a>
                </td>
              </tr>
              <tr>
                <td className="spacer-right">Groups</td>
                <td className="spacer-left bordered-left">{p.groupsCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showGroups.bind(this, p.key)} className="icon-bullet-list" title="Update Users"
                     data-toggle="tooltip" href="#"></a>
                </td>
              </tr>
              </tbody>
            </table>
          </td>
      );
    });
    return (
        <tr>
          <td>
            <span className="little-spacer-right">
              <QualifierIcon qualifier={this.props.project.qualifier}/>
            </span>
            <a href={getComponentUrl(this.props.project.key)}>{this.props.project.name}</a>
          </td>
          {permissions}
          <td className="thin nowrap text-right">
            <button onClick={this.applyTemplate} className="js-apply-template">Apply Template</button>
          </td>
        </tr>
    );
  }
});
