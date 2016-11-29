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
import classNames from 'classnames';
import React from 'react';
import { translate } from '../../../../helpers/l10n';
import { getComponentUrl } from '../../../../helpers/urls';

const SETTINGS_URLS = [
  '/project/settings',
  '/project/quality_profiles',
  '/project/quality_gate',
  '/custom_measures',
  '/project/links',
  '/project_roles',
  '/project/history',
  'background_tasks',
  '/project/key',
  '/project/deletion'
];

export default class ComponentNavMenu extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired,
    conf: React.PropTypes.object.isRequired
  };

  isDeveloper () {
    return this.props.component.qualifier === 'DEV';
  }

  isView () {
    const { qualifier } = this.props.component;
    return qualifier === 'VW' || qualifier === 'SVW';
  }

  isFixedDashboardActive () {
    const path = window.location.pathname;
    return path.indexOf(window.baseUrl + '/dashboard') === 0 || path.indexOf(window.baseUrl + '/governance') === 0;
  }

  shouldShowAdministration () {
    return Object.keys(this.props.conf).some(key => this.props.conf[key]);
  }

  renderLink (url, title, highlightUrl = url) {
    const fullUrl = window.baseUrl + url;
    const isActive = typeof highlightUrl === 'string' ?
    window.location.pathname.indexOf(window.baseUrl + highlightUrl) === 0 :
        highlightUrl(fullUrl);

    return (
        <li key={url} className={classNames({ 'active': isActive })}>
          <a href={fullUrl}>{title}</a>
        </li>
    );
  }

  renderDashboardLink () {
    const url = getComponentUrl(this.props.component.key);
    const name = <i className="icon-home"/>;
    const className = classNames({ active: this.isFixedDashboardActive() });
    return (
        <li key="overview" className={className}>
          <a href={url}>{name}</a>
        </li>
    );
  }

  renderCodeLink () {
    if (this.isDeveloper()) {
      return null;
    }

    const url = `/code/?id=${encodeURIComponent(this.props.component.key)}`;
    const header = this.isView() ? translate('view_projects.page') : translate('code.page');
    return this.renderLink(url, header, '/code');
  }

  renderComponentIssuesLink () {
    const url = `/component_issues?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('issues.page'), '/component_issues');
  }

  renderComponentMeasuresLink () {
    const url = `/component_measures/?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('layout.measures'), '/component_measures');
  }

  renderAdministration () {
    if (!this.shouldShowAdministration()) {
      return null;
    }
    const isSettingsActive = SETTINGS_URLS.some(url => window.location.href.indexOf(url) !== -1);
    const className = 'dropdown' + (isSettingsActive ? ' active' : '');
    return (
        <li className={className}>
          <a className="dropdown-toggle navbar-admin-link" data-toggle="dropdown" href="#">
            {translate('layout.settings')}&nbsp;
            <i className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {this.renderSettingsLink()}
            {this.renderProfilesLink()}
            {this.renderQualityGateLink()}
            {this.renderCustomMeasuresLink()}
            {this.renderLinksLink()}
            {this.renderPermissionsLink()}
            {this.renderHistoryLink()}
            {this.renderBackgroundTasksLink()}
            {this.renderUpdateKeyLink()}
            {this.renderExtensions()}
            {this.renderDeletionLink()}
          </ul>
        </li>
    );
  }

  renderSettingsLink () {
    if (!this.props.conf.showSettings) {
      return null;
    }
    const url = `/project/settings?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('project_settings.page'), '/project/settings');
  }

  renderProfilesLink () {
    if (!this.props.conf.showQualityProfiles) {
      return null;
    }
    const url = `/project/quality_profiles?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('project_quality_profiles.page'), '/project/quality_profiles');
  }

  renderQualityGateLink () {
    if (!this.props.conf.showQualityGates) {
      return null;
    }
    const url = `/project/quality_gate?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('project_quality_gate.page'), '/project/quality_gate');
  }

  renderCustomMeasuresLink () {
    if (!this.props.conf.showManualMeasures) {
      return null;
    }
    const url = `/custom_measures?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('custom_measures.page'), '/custom_measures');
  }

  renderLinksLink () {
    if (!this.props.conf.showLinks) {
      return null;
    }
    const url = `/project/links?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('project_links.page'), '/project/links');
  }

  renderPermissionsLink () {
    if (!this.props.conf.showPermissions) {
      return null;
    }
    const url = `/project_roles?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('permissions.page'), '/project_roles');
  }

  renderHistoryLink () {
    if (!this.props.conf.showHistory) {
      return null;
    }
    const url = `/project/history?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('project_history.page'), '/project/history');
  }

  renderBackgroundTasksLink () {
    if (!this.props.conf.showBackgroundTasks) {
      return null;
    }
    const url = `/project/background_tasks?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('background_tasks.page'), '/project/background_tasks');
  }

  renderUpdateKeyLink () {
    if (!this.props.conf.showUpdateKey) {
      return null;
    }
    const url = `/project/key?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('update_key.page'), '/project/key');
  }

  renderDeletionLink () {
    const { qualifier } = this.props.component;

    if (qualifier !== 'TRK' && qualifier !== 'VW') {
      return null;
    }

    const url = `/project/deletion?id=${encodeURIComponent(this.props.component.key)}`;
    return this.renderLink(url, translate('deletion.page'), '/project/deletion');
  }

  renderExtensions () {
    const extensions = this.props.conf.extensions || [];
    return extensions.map(e => this.renderLink(e.url, e.name, e.url));
  }

  renderTools () {
    const extensions = this.props.component.extensions || [];
    const withoutGovernance = extensions.filter(ext => ext.name !== 'Governance');
    const tools = withoutGovernance
        .map(extension => this.renderLink(extension.url, extension.name));

    if (!tools.length) {
      return null;
    }

    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            {translate('more')}&nbsp;
            <i className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu">
            {tools}
          </ul>
        </li>
    );
  }

  render () {
    return (
        <ul className="nav navbar-nav nav-tabs">
          {this.renderDashboardLink()}
          {this.renderComponentIssuesLink()}
          {this.renderComponentMeasuresLink()}
          {this.renderCodeLink()}
          {this.renderTools()}
          {this.renderAdministration()}
        </ul>
    );
  }
}
