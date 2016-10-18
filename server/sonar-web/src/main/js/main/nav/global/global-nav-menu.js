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
import DashboardNameMixin from '../dashboard-name-mixin';
import LinksMixin from '../links-mixin';
import { translate } from '../../../helpers/l10n';

export default React.createClass({
  mixins: [DashboardNameMixin, LinksMixin],

  getDefaultProps () {
    return { globalDashboards: [], globalPages: [] };
  },

  renderDashboardLink (dashboard) {
    const url = `${window.baseUrl}/dashboard/index?did=${encodeURIComponent(dashboard.key)}`;
    const name = this.getLocalizedDashboardName(dashboard.name);
    return (
        <li key={dashboard.name}>
          <a href={url}>{name}</a>
        </li>
    );
  },

  renderDashboardsManagementLink () {
    const url = window.baseUrl + '/dashboards';
    return (
        <li>
          <a href={url}>{translate('dashboard.manage_dashboards')}</a>
        </li>
    );
  },

  renderDashboards () {
    if (window.SS.user) {
      // do not render dashboards menu for authenticated users
      return null;
    }

    const dashboards = this.props.globalDashboards.map(this.renderDashboardLink);
    const canManageDashboards = !!window.SS.user;
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {translate('layout.dashboards')}&nbsp;
            <span className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {dashboards}
            {canManageDashboards ? <li className="divider"/> : null}
            {canManageDashboards ? this.renderDashboardsManagementLink() : null}
          </ul>
        </li>
    );
  },

  renderProjects () {
    const url = window.baseUrl + '/projects';
    return (
        <li className={this.activeLink('/projects')}>
          <a href={url}>{translate('projects.page')}</a>
        </li>
    );
  },

  renderIssuesLink () {
    const url = window.baseUrl + '/issues/search';
    return (
        <li className={this.activeLink('/issues')}>
          <a href={url}>{translate('issues.page')}</a>
        </li>
    );
  },

  renderMeasuresLink () {
    const url = window.baseUrl + '/measures/search?qualifiers[]=TRK';
    return (
        <li className={this.activeLink('/measures')}>
          <a href={url}>{translate('layout.measures')}</a>
        </li>
    );
  },

  renderRulesLink () {
    const url = window.baseUrl + '/coding_rules';
    return (
        <li className={this.activeLink('/coding_rules')}>
          <a href={url}>{translate('coding_rules.page')}</a>
        </li>
    );
  },

  renderProfilesLink() {
    const url = window.baseUrl + '/profiles';
    return (
        <li className={this.activeLink('/profiles')}>
          <a href={url}>{translate('quality_profiles.page')}</a>
        </li>
    );
  },

  renderQualityGatesLink () {
    const url = window.baseUrl + '/quality_gates';
    return (
        <li className={this.activeLink('/quality_gates')}>
          <a href={url}>{translate('quality_gates.page')}</a>
        </li>
    );
  },

  renderAdministrationLink () {
    if (!window.SS.isUserAdmin) {
      return null;
    }
    const url = window.baseUrl + '/settings';
    return (
        <li className={this.activeLink('/settings')}>
          <a className="navbar-admin-link" href={url}>{translate('layout.settings')}</a>
        </li>
    );
  },

  renderGlobalPageLink (globalPage, index) {
    const url = window.baseUrl + globalPage.url;
    return (
        <li key={index}>
          <a href={url}>{globalPage.name}</a>
        </li>
    );
  },

  renderMore () {
    if (this.props.globalPages.length === 0) {
      return null;
    }
    const globalPages = this.props.globalPages.map(this.renderGlobalPageLink);
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {translate('more')}&nbsp;
            <span className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {globalPages}
          </ul>
        </li>
    );
  },

  render () {
    return (
        <ul className="nav navbar-nav">
          {this.renderDashboards()}
          {this.renderProjects()}
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
