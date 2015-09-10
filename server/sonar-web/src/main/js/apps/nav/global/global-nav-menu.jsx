import React from 'react';
import DashboardNameMixin from '../dashboard-name-mixin';
import LinksMixin from '../links-mixin';

export default React.createClass({
  mixins: [DashboardNameMixin, LinksMixin],

  getDefaultProps: function () {
    return { globalDashboards: [], globalPages: [] };
  },

  renderDashboardLink(dashboard) {
    const url = `${window.baseUrl}/dashboard/index?did=${encodeURIComponent(dashboard.key)}`;
    const name = this.getLocalizedDashboardName(dashboard.name);
    return (
        <li key={dashboard.name}>
          <a href={url}>{name}</a>
        </li>
    );
  },

  renderDashboardsManagementLink() {
    const url = `${window.baseUrl}/dashboards`;
    return (
        <li>
          <a href={url}>{window.t('dashboard.manage_dashboards')}</a>
        </li>
    );
  },

  renderDashboards() {
    const dashboards = this.props.globalDashboards.map(this.renderDashboardLink);
    const canManageDashboards = !!window.SS.user;
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {window.t('layout.dashboards')}&nbsp;<span className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {dashboards}
            {canManageDashboards ? <li className="divider"/> : null}
            {canManageDashboards ? this.renderDashboardsManagementLink() : null}
          </ul>
        </li>
    );
  },

  renderIssuesLink() {
    const url = `${window.baseUrl}/issues/search`;
    return (
        <li className={this.activeLink('/issues')}>
          <a href={url}>{window.t('issues.page')}</a>
        </li>
    );
  },

  renderMeasuresLink() {
    const url = `${window.baseUrl}/measures/search?qualifiers[]=TRK`;
    return (
        <li className={this.activeLink('/measures')}>
          <a href={url}>{window.t('layout.measures')}</a>
        </li>
    );
  },

  renderRulesLink() {
    const url = `${window.baseUrl}/coding_rules`;
    return (
        <li className={this.activeLink('/coding_rules')}>
          <a href={url}>{window.t('coding_rules.page')}</a>
        </li>
    );
  },

  renderProfilesLink() {
    const url = `${window.baseUrl}/profiles`;
    return (
        <li className={this.activeLink('/profiles')}>
          <a href={url}>{window.t('quality_profiles.page')}</a>
        </li>
    );
  },

  renderQualityGatesLink() {
    const url = `${window.baseUrl}/quality_gates`;
    return (
        <li className={this.activeLink('/quality_gates')}>
          <a href={url}>{window.t('quality_gates.page')}</a>
        </li>
    );
  },

  renderAdministrationLink() {
    if (!window.SS.isUserAdmin) {
      return null;
    }
    const url = `${window.baseUrl}/settings`;
    return (
        <li className={this.activeLink('/settings')}>
          <a className="navbar-admin-link" href={url}>{window.t('layout.settings')}</a>
        </li>
    );
  },

  renderComparisonLink() {
    const url = `${window.baseUrl}/comparison`;
    return (
        <li className={this.activeLink('/comparison')}>
          <a href={url}>{window.t('comparison_global.page')}</a>
        </li>
    );
  },

  renderGlobalPageLink(globalPage, index) {
    const url = window.baseUrl + globalPage.url;
    return (
        <li key={index}>
          <a href={url}>{globalPage.name}</a>
        </li>
    );
  },

  renderMore() {
    const globalPages = this.props.globalPages.map(this.renderGlobalPageLink);
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {window.t('more')}&nbsp;<span className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {this.renderComparisonLink()}
            {globalPages}
          </ul>
        </li>
    );
  },

  render() {
    return (
        <ul className="nav navbar-nav">
          {this.renderDashboards()}
          {this.renderIssuesLink()}
          {this.renderMeasuresLink()}
          {this.renderRulesLink()}
          {this.renderProfilesLink()}
          {this.renderQualityGatesLink()}
          {this.renderAdministrationLink()}
          {this.renderMore()}
        </ul>
    );
  }
});
