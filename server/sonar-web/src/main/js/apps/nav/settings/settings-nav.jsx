import $ from 'jquery';
import React from 'react';
import LinksMixin from '../links-mixin';

export default React.createClass({
  mixins: [LinksMixin],

  getInitialState() {
    return { extensions: [] };
  },

  componentDidMount() {
    this.loadDetails();
  },

  loadDetails() {
    $.get(`${window.baseUrl}/api/navigation/settings`).done(r => this.setState(r));
  },

  render() {
    return (
        <div className="container">
          <ul className="nav navbar-nav nav-crumbs">
            {this.renderLink('/settings', window.t('layout.settings'))}
          </ul>

          <ul className="nav navbar-nav nav-tabs">
            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {window.t('sidebar.project_settings')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/settings', window.t('settings.page'))}
                {this.renderLink('/metrics', 'Custom Metrics')}
                {this.renderLink('/admin_dashboards', window.t('default_dashboards.page'))}
                {this.state.extensions.map(e => this.renderLink(e.url, e.name))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {window.t('sidebar.security')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/users', window.t('users.page'))}
                {this.renderLink('/groups', window.t('user_groups.page'))}
                {this.renderLink('/roles/global', window.t('global_permissions.page'))}
                {this.renderLink('/roles/projects', window.t('roles.page'))}
                {this.renderLink('/permission_templates', window.t('permission_templates'))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {window.t('sidebar.projects')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.state.showProvisioning ? this.renderLink('/provisioning', window.t('provisioning.page')) : null}
                {this.renderLink('/bulk_deletion', window.t('bulk_deletion.page'))}
                {this.renderLink('/computation', window.t('analysis_reports.page'))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {window.t('sidebar.system')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/updatecenter', window.t('update_center.page'))}
                {this.renderLink('/system', window.t('system_info.page'))}
              </ul>
            </li>
          </ul>
        </div>

    );
  }
});
