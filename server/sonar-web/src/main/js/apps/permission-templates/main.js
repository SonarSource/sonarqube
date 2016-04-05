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
import React from 'react';
import Header from './header';
import PermissionTemplates from './permission-templates';
import { getPermissionTemplates } from '../../api/permissions';

const PERMISSIONS_ORDER = ['user', 'codeviewer', 'issueadmin', 'admin', 'scan'];

export default React.createClass({
  propTypes: {
    topQualifiers: React.PropTypes.array.isRequired
  },

  getInitialState() {
    return { ready: false, permissions: [], permissionTemplates: [] };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  sortPermissions(permissions) {
    return _.sortBy(permissions, p => PERMISSIONS_ORDER.indexOf(p.key));
  },

  mergePermissionsToTemplates(permissionTemplates, basePermissions) {
    return permissionTemplates.map(permissionTemplate => {
      // it's important to keep the order of the permission template's permissions
      // the same as the order of base permissions
      const permissions = basePermissions.map(basePermission => {
        const projectPermission = _.findWhere(permissionTemplate.permissions, { key: basePermission.key });
        return _.extend({ usersCount: 0, groupsCount: 0 }, basePermission, projectPermission);
      });
      return _.extend({}, permissionTemplate, { permissions });
    });
  },

  mergeDefaultsToTemplates(permissionTemplates, defaultTemplates = []) {
    return permissionTemplates.map(permissionTemplate => {
      const defaultFor = [];
      defaultTemplates.forEach(defaultTemplate => {
        if (defaultTemplate.templateId === permissionTemplate.id) {
          defaultFor.push(defaultTemplate.qualifier);
        }
      });
      return _.extend({}, permissionTemplate, { defaultFor });
    });
  },

  requestPermissions() {
    getPermissionTemplates().done(r => {
      const permissions = this.sortPermissions(r.permissions);
      const permissionTemplates = this.mergePermissionsToTemplates(r.permissionTemplates, permissions);
      const permissionTemplatesWithDefaults = this.mergeDefaultsToTemplates(permissionTemplates, r.defaultTemplates);
      this.setState({
        ready: true,
        permissionTemplates: permissionTemplatesWithDefaults,
        permissions
      });
    });
  },

  render() {
    return (
        <div className="page">
          <Header ready={this.state.ready} refresh={this.requestPermissions}/>

          <PermissionTemplates
              ready={this.state.ready}
              permissionTemplates={this.state.permissionTemplates}
              permissions={this.state.permissions}
              topQualifiers={this.props.topQualifiers}
              refresh={this.requestPermissions}/>
        </div>
    );
  }
});
