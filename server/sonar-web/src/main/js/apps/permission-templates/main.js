import _ from 'underscore';
import React from 'react';
import Header from './header';
import PermissionTemplates from './permission-templates';
import { getPermissionTemplates } from '../../api/permissions';

const PERMISSIONS_ORDER = ['user', 'codeviewer', 'issueadmin', 'admin'];

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
      let permissions = basePermissions.map(basePermission => {
        let projectPermission = _.findWhere(permissionTemplate.permissions, { key: basePermission.key });
        return _.extend({ usersCount: 0, groupsCount: 0 }, basePermission, projectPermission);
      });
      return _.extend({}, permissionTemplate, { permissions: permissions });
    });
  },

  mergeDefaultsToTemplates(permissionTemplates, defaultTemplates = []) {
    return permissionTemplates.map(permissionTemplate => {
      let defaultFor = [];
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
      let permissions = this.sortPermissions(r.permissions);
      let permissionTemplates = this.mergePermissionsToTemplates(r.permissionTemplates, permissions);
      let permissionTemplatesWithDefaults = this.mergeDefaultsToTemplates(permissionTemplates, r.defaultTemplates);
      this.setState({
        ready: true,
        permissionTemplates: permissionTemplatesWithDefaults,
        permissions: permissions
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
