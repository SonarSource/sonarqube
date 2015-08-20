import React from 'react';
import Permissions from './permissions';
import PermissionsFooter from './permissions-footer';
import Search from './search';

let $ = jQuery;

export default React.createClass({
  getInitialState() {
    return { permissions: [], projects: [], total: 0 };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  mergePermissionsToProjects(projects, basePermissions) {
    return projects.map(project => {
      // it's important to keep the order of the project permissions the same as the order of base permissions
      let permissions = basePermissions.map(basePermission => {
        let projectPermission = _.findWhere(project.permissions, { key: basePermission.key });
        if (!projectPermission) {
          throw new Error(`Project "${project.name} [${project.key}]" doesn't have permission "${basePermission.key}"`);
        }
        return _.extend({}, basePermission, projectPermission);
      });
      return _.extend({}, project, { permissions: permissions });
    });
  },

  requestPermissions(page = 1, query = '') {
    let url = `${window.baseUrl}/api/permissions/search_project_permissions`;
    let data = { p: page, q: query };
    $.get(url, data).done(r => {
      let projects = this.mergePermissionsToProjects(r.projects, r.permissions);
      if (page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({
        projects: projects,
        permissions: r.permissions,
        total: r.paging.total,
        page: r.paging.pageIndex,
        query: query
      });
    });
  },

  loadMore() {
    this.requestPermissions(this.state.page + 1, this.state.query);
  },

  search(query) {
    this.requestPermissions(1, query);
  },

  render() {
    return (
        <div className="page">
          <header id="project-permissions-header" className="page-header">
            <h1 className="page-title">{window.t('roles.page')}</h1>
            <p className="page-description">{window.t('roles.page.description2')}</p>
          </header>

          <Search
              search={this.search}/>

          <Permissions
              projects={this.state.projects}
              permissions={this.state.permissions}
              refresh={this.requestPermissions}/>

          <PermissionsFooter
              count={this.state.projects.length}
              total={this.state.total}
              loadMore={this.loadMore}/>
        </div>
    );
  }
});
