/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import { Link } from 'react-router';
import { translate } from '../../../../helpers/l10n';
import { isUserAdmin } from '../../../../helpers/users';

export default class GlobalNavMenu extends React.Component {
  static propTypes = {
    appState: React.PropTypes.object.isRequired,
    currentUser: React.PropTypes.object.isRequired
  };

  static defaultProps = {
    globalDashboards: [],
    globalPages: []
  };

  activeLink(url) {
    return window.location.pathname.indexOf(window.baseUrl + url) === 0 ? 'active' : null;
  }

  renderProjects() {
    return (
      <li>
        <Link to="/projects" activeClassName="active">
          {translate('projects.page')}
        </Link>
      </li>
    );
  }

  renderIssuesLink() {
    const query = this.props.currentUser.isLoggedIn
      ? '#resolved=false|assigned_to_me=true'
      : '#resolved=false';
    const url = '/issues' + query;
    return (
      <li>
        <Link to={url} className={this.activeLink('/issues')}>
          {translate('issues.page')}
        </Link>
      </li>
    );
  }

  renderRulesLink() {
    return (
      <li>
        <Link to="/coding_rules" className={this.activeLink('/coding_rules')}>
          {translate('coding_rules.page')}
        </Link>
      </li>
    );
  }

  renderProfilesLink() {
    return (
      <li>
        <Link to="/profiles" activeClassName="active">
          {translate('quality_profiles.page')}
        </Link>
      </li>
    );
  }

  renderQualityGatesLink() {
    return (
      <li>
        <Link to="/quality_gates" activeClassName="active">
          {translate('quality_gates.page')}
        </Link>
      </li>
    );
  }

  renderAdministrationLink() {
    if (!isUserAdmin(this.props.currentUser)) {
      return null;
    }
    return (
      <li>
        <Link to="/settings" className="navbar-admin-link" activeClassName="active">
          {translate('layout.settings')}
        </Link>
      </li>
    );
  }

  renderGlobalPageLink = ({ key, name }) => {
    return (
      <li key={key}>
        <Link to={`/extension/${key}`}>{name}</Link>
      </li>
    );
  };

  renderMore() {
    const { globalPages } = this.props.appState;
    if (globalPages.length === 0) {
      return null;
    }
    return (
      <li className="dropdown">
        <a className="dropdown-toggle" id="global-navigation-more" data-toggle="dropdown" href="#">
          {translate('more')}&nbsp;
          <span className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu">
          {globalPages.map(this.renderGlobalPageLink)}
        </ul>
      </li>
    );
  }

  render() {
    return (
      <ul className="nav navbar-nav">
        {this.renderProjects()}
        {this.renderIssuesLink()}
        {this.renderRulesLink()}
        {this.renderProfilesLink()}
        {this.renderQualityGatesLink()}
        {this.renderAdministrationLink()}
        {this.renderMore()}
      </ul>
    );
  }
}
