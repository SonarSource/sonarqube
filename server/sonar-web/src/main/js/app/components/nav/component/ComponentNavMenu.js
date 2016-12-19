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
import { Link } from 'react-router';
import { translate } from '../../../../helpers/l10n';

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
    return (
        <li>
          <Link
              to={{ pathname: '/dashboard', query: { id: this.props.component.key } }}
              activeClassName="active">
            <i className="icon-home"/>
          </Link>
        </li>
    );
  }

  renderCodeLink () {
    if (this.isDeveloper()) {
      return null;
    }

    return (
        <li>
          <Link
              to={{ pathname: '/code', query: { id: this.props.component.key } }}
              activeClassName="active">
            {this.isView() ? translate('view_projects.page') : translate('code.page')}
          </Link>
        </li>
    );
  }

  renderActivityLink () {
    return (
        <li>
          <Link to={{ pathname: '/project/activity', query: { id: this.props.component.key } }}
                activeClassName="active">
            {translate('project_activity.page')}
          </Link>
        </li>
    );
  }

  renderComponentIssuesLink () {
    return (
        <li>
          <Link
              to={{ pathname: '/component_issues', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('issues.page')}
          </Link>
        </li>
    );
  }

  renderComponentMeasuresLink () {
    return (
        <li>
          <Link
              to={{ pathname: '/component_measures', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('layout.measures')}
          </Link>
        </li>
    );
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
    return (
        <li>
          <Link
              to={{ pathname: '/project/settings', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('project_settings.page')}
          </Link>
        </li>
    );
  }

  renderProfilesLink () {
    if (!this.props.conf.showQualityProfiles) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project/quality_profiles', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('project_quality_profiles.page')}
          </Link>
        </li>
    );
  }

  renderQualityGateLink () {
    if (!this.props.conf.showQualityGates) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project/quality_gate', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('project_quality_gate.page')}
          </Link>
        </li>
    );
  }

  renderCustomMeasuresLink () {
    if (!this.props.conf.showManualMeasures) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/custom_measures', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('custom_measures.page')}
          </Link>
        </li>
    );
  }

  renderLinksLink () {
    if (!this.props.conf.showLinks) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project/links', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('project_links.page')}
          </Link>
        </li>
    );
  }

  renderPermissionsLink () {
    if (!this.props.conf.showPermissions) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project_roles', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('permissions.page')}
          </Link>
        </li>
    );
  }

  renderBackgroundTasksLink () {
    if (!this.props.conf.showBackgroundTasks) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project/background_tasks', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('background_tasks.page')}
          </Link>
        </li>
    );
  }

  renderUpdateKeyLink () {
    if (!this.props.conf.showUpdateKey) {
      return null;
    }
    return (
        <li>
          <Link
              to={{ pathname: '/project/key', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('update_key.page')}
          </Link>
        </li>
    );
  }

  renderDeletionLink () {
    const { qualifier } = this.props.component;

    if (qualifier !== 'TRK' && qualifier !== 'VW') {
      return null;
    }

    return (
        <li>
          <Link
              to={{ pathname: '/project/deletion', query: { id: this.props.component.key } }}
              activeClassName="active">
            {translate('deletion.page')}
          </Link>
        </li>
    );
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
          {this.renderActivityLink()}
          {this.renderTools()}
          {this.renderAdministration()}
        </ul>
    );
  }
}
