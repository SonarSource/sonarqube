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
import _ from 'underscore';
import Backbone from 'backbone';
import React from 'react';
import Defaults from './permission-template-defaults';
import SetDefaults from './permission-template-set-defaults';
import UsersView from './users-view';
import GroupsView from './groups-view';
import UpdateView from './update-view';
import DeleteView from './delete-view';

export default React.createClass({
  propTypes: {
    permissionTemplate: React.PropTypes.object.isRequired,
    topQualifiers: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  showGroups(permission, e) {
    e.preventDefault();
    new GroupsView({
      permission: permission,
      permissionTemplate: this.props.permissionTemplate,
      refresh: this.props.refresh
    }).render();
  },

  showUsers(permission, e) {
    e.preventDefault();
    new UsersView({
      permission: permission,
      permissionTemplate: this.props.permissionTemplate,
      refresh: this.props.refresh
    }).render();
  },

  onUpdate(e) {
    e.preventDefault();
    new UpdateView({
      model: new Backbone.Model(this.props.permissionTemplate),
      refresh: this.props.refresh
    }).render();
  },

  onDelete(e) {
    e.preventDefault();
    new DeleteView({
      model: new Backbone.Model(this.props.permissionTemplate),
      refresh: this.props.refresh
    }).render();
  },

  renderAssociation() {
    let projectKeyPattern = this.props.permissionTemplate.projectKeyPattern;
    if (!projectKeyPattern) {
      return null;
    }
    return <div className="spacer-bottom">Project Key Pattern: <code>{projectKeyPattern}</code></div>;
  },

  renderDeleteButton() {
    if (_.size(this.props.permissionTemplate.defaultFor) > 0) {
      return null;
    }
    return <button onClick={this.onDelete} className="button-red">Delete</button>;
  },

  render() {
    let permissions = this.props.permissionTemplate.permissions.map(p => {
      return (
          <td key={p.key}>
            <table>
              <tbody>
              <tr>
                <td className="spacer-right">Users</td>
                <td className="spacer-left bordered-left">{p.usersCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showUsers.bind(this, p)} className="icon-bullet-list" title="Update Users"
                     data-toggle="tooltip" href="#"></a>
                </td>
              </tr>
              <tr>
                <td className="spacer-right">Groups</td>
                <td className="spacer-left bordered-left">{p.groupsCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showGroups.bind(this, p)} className="icon-bullet-list" title="Update Users"
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
            <strong>{this.props.permissionTemplate.name}</strong>
            <p className="note little-spacer-top">{this.props.permissionTemplate.description}</p>
          </td>
          {permissions}
          <td className="thin text-right">
            {this.renderAssociation()}
            <Defaults
                permissionTemplate={this.props.permissionTemplate}
                topQualifiers={this.props.topQualifiers}/>
            <div className="nowrap">
              <SetDefaults
                  permissionTemplate={this.props.permissionTemplate}
                  topQualifiers={this.props.topQualifiers}
                  refresh={this.props.refresh}/>

              <div className="button-group">
                <button onClick={this.onUpdate}>Update</button>
                {this.renderDeleteButton()}
              </div>
            </div>
          </td>
        </tr>
    );
  }
});
