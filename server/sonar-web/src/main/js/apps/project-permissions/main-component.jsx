import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import PermissionsList from '../global-permissions/permissions-list';

const PERMISSIONS_ORDER = ['user', 'codeviewer', 'issueadmin', 'admin'];

export default React.createClass({
  getInitialState() {
    return { permissions: [] };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  sortPermissions(permissions) {
    return _.sortBy(permissions, p => PERMISSIONS_ORDER.indexOf(p.key));
  },

  mergePermissionsToProjects(projects, basePermissions) {
    return projects.map(project => {
      // it's important to keep the order of the project permissions the same as the order of base permissions
      let permissions = basePermissions.map(basePermission => {
        let projectPermission = _.findWhere(project.permissions, { key: basePermission.key });
        return _.extend({ usersCount: 0, groupsCount: 0 }, basePermission, projectPermission);
      });
      return _.extend({}, project, { permissions: permissions });
    });
  },

  requestPermissions(page = 1, query = '') {
    let url = `${window.baseUrl}/api/permissions/search_project_permissions`;
    let data = { projectId: this.props.component.uuid, p: page, q: query };
    $.get(url, data).done(r => {
      let permissions = this.sortPermissions(r.permissions);
      let projects = this.mergePermissionsToProjects(r.projects, permissions);
      this.setState({ permissions: projects[0].permissions });
    });
  },

  render() {
    return (
        <div className="page">
          <header id="project-permissions-header" className="page-header">
            <h1 className="page-title">{window.t('roles.page')}</h1>
            <p className="page-description">{window.t('roles.page.description2')}</p>
          </header>

          <PermissionsList permissions={this.state.permissions} project={this.props.component.uuid}/>
        </div>
    );
  }
});
