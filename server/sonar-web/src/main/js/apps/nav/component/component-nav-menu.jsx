import _ from 'underscore';
import React from 'react';
import DashboardNameMixin from '../dashboard-name-mixin';
import LinksMixin from '../links-mixin';

const SETTINGS_URLS = [
  '/project/settings', '/project/profile', '/project/qualitygate', '/manual_measures/index',
  '/action_plans/index', '/project/links', '/project_roles/index', '/project/history', '/project/key',
  '/project/deletion'
];

export default React.createClass({
  mixins: [DashboardNameMixin, LinksMixin],

  renderOverviewLink() {
    if (_.size(this.props.component.dashboards) === 0) {
      return null;
    }
    let firstDashboard = _.first(this.props.component.dashboards);
    let url = `/dashboard/index?id=${encodeURIComponent(this.props.component.key)}`;
    let name = this.getLocalizedDashboardName(firstDashboard.name);
    return this.renderLink(url, name, () => {
      /* eslint eqeqeq: 0 */
      let pathMatch = window.location.pathname === `${window.baseUrl}/dashboard` ||
          window.location.pathname === `${window.baseUrl}/dashboard/index`;
      let params = window.getQueryParams();
      let paramMatch = !params['did'] || params['did'] == firstDashboard.key;
      return pathMatch && paramMatch ? 'active' : null;
    });
  },

  renderComponentsLink() {
    const url = `/components/index?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('components.page'), '/components');
  },

  renderComponentIssuesLink() {
    const url = `/component_issues/index?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('issues.page'), '/component_issues');
  },

  renderAdministration() {
    let shouldShowAdministration =
        this.props.conf.showActionPlans ||
        this.props.conf.showDeletion ||
        this.props.conf.showHistory ||
        this.props.conf.showLinks ||
        this.props.conf.showManualMeasures ||
        this.props.conf.showPermissions ||
        this.props.conf.showQualityGates ||
        this.props.conf.showQualityProfiles ||
        this.props.conf.showSettings ||
        this.props.conf.showUpdateKey;
    if (!shouldShowAdministration) {
      return null;
    }
    let isSettingsActive = SETTINGS_URLS.some(url => {
          return window.location.href.indexOf(url) !== -1;
        }),
        className = 'dropdown' + (isSettingsActive ? ' active' : '');
    return (
        <li className={className}>
          <a className="dropdown-toggle navbar-admin-link" data-toggle="dropdown" href="#">
            {window.t('layout.settings')}&nbsp;<i className="icon-dropdown"/></a>
          <ul className="dropdown-menu">
            {this.renderSettingsLink()}
            {this.renderProfilesLink()}
            {this.renderQualityGatesLink()}
            {this.renderCustomMeasuresLink()}
            {this.renderActionPlansLink()}
            {this.renderLinksLink()}
            {this.renderPermissionsLink()}
            {this.renderHistoryLink()}
            {this.renderUpdateKeyLink()}
            {this.renderDeletionLink()}
            {this.renderExtensions()}
          </ul>
        </li>
    );
  },

  renderSettingsLink() {
    if (!this.props.conf.showSettings) {
      return null;
    }
    const url = `/project/settings?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('project_settings.page'), '/project/settings');
  },

  renderProfilesLink() {
    if (!this.props.conf.showQualityProfiles) {
      return null;
    }
    const url = `/project/profile?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('project_quality_profiles.page'), '/project/profile');
  },

  renderQualityGatesLink() {
    if (!this.props.conf.showQualityGates) {
      return null;
    }
    const url = `/project/qualitygate?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('project_quality_gate.page'), '/project/qualitygate');
  },

  renderCustomMeasuresLink() {
    if (!this.props.conf.showManualMeasures) {
      return null;
    }
    const url = `/custom_measures?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('custom_measures.page'), '/custom_measures');
  },

  renderActionPlansLink() {
    if (!this.props.conf.showActionPlans) {
      return null;
    }
    const url = `/action_plans?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('action_plans.page'), '/action_plans');
  },

  renderLinksLink() {
    if (!this.props.conf.showLinks) {
      return null;
    }
    const url = `/project/links?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('project_links.page'), '/project/links');
  },

  renderPermissionsLink() {
    if (!this.props.conf.showPermissions) {
      return null;
    }
    const url = `/project_roles?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('permissions.page'), '/project_roles');
  },

  renderHistoryLink() {
    if (!this.props.conf.showHistory) {
      return null;
    }
    const url = `/project/history?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('project_history.page'), '/project/history');
  },

  renderUpdateKeyLink() {
    if (!this.props.conf.showUpdateKey) {
      return null;
    }
    const url = `/project/key?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('update_key.page'), '/project/key');
  },

  renderDeletionLink() {
    if (!this.props.conf.showDeletion) {
      return null;
    }
    const url = `/project/deletion?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, window.t('deletion.page'), '/project/deletion');
  },

  renderExtensions() {
    let extensions = this.props.conf.extensions || [];
    return extensions.map(e => {
      return this.renderLink(e.url, e.name, e.url);
    });
  },

  renderMore() {
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {window.t('more')}&nbsp;<i className="icon-dropdown"></i>
          </a>
          <ul className="dropdown-menu">
            {this.renderDashboards()}
            {this.renderDashboardManagementLink()}
            {this.renderTools()}
          </ul>
        </li>
    );
  },

  renderDashboards() {
    let dashboards = _.rest(this.props.component.dashboards || []).map(d => {
      let url = `/dashboard?id=${encodeURIComponent(this.props.component.key)}&did=${d.key}`;
      let name = this.getLocalizedDashboardName(d.name);
      return this.renderLink(url, name);
    });
    return [<li key="0" className="dropdown-header">{window.t('layout.dashboards')}</li>].concat(dashboards);
  },

  renderDashboardManagementLink() {
    if (!window.SS.user) {
      return null;
    }
    let url = `/dashboards?resource=${encodeURIComponent(this.props.component.key)}`;
    let name = window.t('dashboard.manage_dashboards');
    return [
      <li key="dashboard-divider" className="small-divider"></li>,
      this.renderLink(url, name, '/dashboards')
    ];
  },

  renderTools() {
    let component = this.props.component;
    if (!component.isComparable && !_.size(component.extensions)) {
      return null;
    }
    let tools = [
      <li key="tools-divider" className="divider"></li>,
      <li key="tools" className="dropdown-header">Tools</li>
    ];
    if (component.isComparable) {
      let compareUrl = `/comparison/index?resource=${component.key}`;
      tools.push(this.renderLink(compareUrl, window.t('comparison.page')));
    }
    (component.extensions || []).forEach(e => {
      tools.push(this.renderLink(e.url, e.name));
    });
    return tools;
  },

  render() {
    return (
        <ul className="nav navbar-nav nav-tabs">
          {this.renderOverviewLink()}
          {this.renderComponentsLink()}
          {this.renderComponentIssuesLink()}
          {this.renderAdministration()}
          {this.renderMore()}
        </ul>
    );
  }
});
