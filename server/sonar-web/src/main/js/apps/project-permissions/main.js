import $ from 'jquery';
import _ from 'underscore';
import React from 'react';

import Permissions from './permissions';
import PermissionsFooter from './permissions-footer';
import Search from './search';
import ApplyTemplateView from './apply-template-view';


const PERMISSIONS_ORDER = ['user', 'codeviewer', 'issueadmin', 'admin'];


export default React.createClass({
  propTypes: {
    permissionTemplates: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  getInitialState() {
    return { ready: false, permissions: [], projects: [], total: 0, filter: '__ALL__' };
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

  requestPermissions(page = 1, query = '', filter = this.state.filter) {
    let url = `${window.baseUrl}/api/permissions/search_project_permissions`;
    let data = { p: page, q: query };
    if (filter !== '__ALL__') {
      data.qualifier = filter;
    }
    if (this.props.componentId) {
      data = { projectId: this.props.componentId };
    }
    this.setState({ ready: false }, () => {
      $.get(url, data).done(r => {
        let permissions = this.sortPermissions(r.permissions);
        let projects = this.mergePermissionsToProjects(r.projects, permissions);
        if (page > 1) {
          projects = [].concat(this.state.projects, projects);
        }
        this.setState({
          ready: true,
          projects: projects,
          permissions: permissions,
          total: r.paging.total,
          page: r.paging.pageIndex,
          query: query,
          filter: filter
        });
      });
    });
  },

  loadMore() {
    this.requestPermissions(this.state.page + 1, this.state.query);
  },

  search(query) {
    this.requestPermissions(1, query);
  },

  handleFilter(filter) {
    this.requestPermissions(1, this.state.query, filter);
  },

  refresh() {
    this.requestPermissions(1, this.state.query);
  },

  bulkApplyTemplate(e) {
    e.preventDefault();
    new ApplyTemplateView({
      projects: this.state.projects,
      permissionTemplates: this.props.permissionTemplates,
      refresh: this.requestPermissions
    }).render();
  },

  renderBulkApplyButton() {
    if (this.props.componentId) {
      return null;
    }
    return (
        <button onClick={this.bulkApplyTemplate} className="js-bulk-apply-template">Bulk Apply Template</button>
    );
  },

  renderSpinner () {
    if (this.state.ready) {
      return null;
    }
    return <i className="spinner"/>;
  },

  render() {
    return (
        <div className="page">
          <header id="project-permissions-header" className="page-header">
            <h1 className="page-title">{window.t('roles.page')}</h1>
            {this.renderSpinner()}
            <div className="page-actions">
              {this.renderBulkApplyButton()}
            </div>
            <p className="page-description">{window.t('roles.page.description2')}</p>
          </header>

          <Search {...this.props}
              filter={this.state.filter}
              search={this.search}
              onFilter={this.handleFilter}/>

          <Permissions
              ready={this.state.ready}
              projects={this.state.projects}
              permissions={this.state.permissions}
              permissionTemplates={this.props.permissionTemplates}
              refresh={this.refresh}/>

          <PermissionsFooter {...this.props}
              ready={this.state.ready}
              count={this.state.projects.length}
              total={this.state.total}
              loadMore={this.loadMore}/>
        </div>
    );
  }
});
