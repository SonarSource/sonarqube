/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { Link } from 'react-router';
import { isLoggedIn, CurrentUser, AppState, Extension } from '../../../../app/types';
import { translate } from '../../../../helpers/l10n';
import { getQualityGatesUrl, getBaseUrl } from '../../../../helpers/urls';
import { isMySet } from '../../../../apps/issues/utils';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
  location: { pathname: string };
  onSonarCloud?: boolean;
}

export default class GlobalNavMenu extends React.PureComponent<Props> {
  activeLink(url: string) {
    return window.location.pathname.indexOf(getBaseUrl() + url) === 0 ? 'active' : undefined;
  }

  renderProjects() {
    if (this.props.onSonarCloud && !isLoggedIn(this.props.currentUser)) {
      return null;
    }

    return (
      <li>
        <Link to="/projects" activeClassName="active">
          {this.props.onSonarCloud ? translate('my_projects') : translate('projects.page')}
        </Link>
      </li>
    );
  }

  renderPortfolios() {
    return (
      <li>
        <Link to="/portfolios" activeClassName="active">
          {translate('portfolios.page')}
        </Link>
      </li>
    );
  }

  renderIssuesLink() {
    if (this.props.onSonarCloud && !isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const active = this.props.location.pathname === 'issues';

    if (this.props.onSonarCloud) {
      return (
        <li>
          <Link
            to={{ pathname: '/issues', query: { resolved: 'false' } }}
            className={active ? 'active' : undefined}>
            {translate('my_issues')}
          </Link>
        </li>
      );
    }

    const query =
      this.props.currentUser.isLoggedIn && isMySet()
        ? { resolved: 'false', myIssues: 'true' }
        : { resolved: 'false' };
    return (
      <li>
        <Link to={{ pathname: '/issues', query }} className={active ? 'active' : undefined}>
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
        <Link to={getQualityGatesUrl()} activeClassName="active">
          {translate('quality_gates.page')}
        </Link>
      </li>
    );
  }

  renderAdministrationLink() {
    if (!this.props.appState.canAdmin) {
      return null;
    }

    return (
      <li>
        <Link to="/admin" activeClassName="active">
          {translate('layout.settings')}
        </Link>
      </li>
    );
  }

  renderGlobalPageLink = ({ key, name }: Extension) => {
    return (
      <li key={key}>
        <Link to={`/extension/${key}`}>{name}</Link>
      </li>
    );
  };

  renderMore() {
    const { globalPages = [] } = this.props.appState;
    const withoutPortfolios = globalPages.filter(page => page.key !== 'governance/portfolios');
    if (withoutPortfolios.length === 0) {
      return null;
    }
    return (
      <li className="dropdown">
        <a className="dropdown-toggle" id="global-navigation-more" data-toggle="dropdown" href="#">
          {translate('more')}&nbsp;
          <span className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu">{withoutPortfolios.map(this.renderGlobalPageLink)}</ul>
      </li>
    );
  }

  render() {
    const governanceInstalled = this.props.appState.qualifiers.includes('VW');
    const { organizationsEnabled } = this.props.appState;

    return (
      <ul className="global-navbar-menu pull-left">
        {this.renderProjects()}
        {governanceInstalled && this.renderPortfolios()}
        {this.renderIssuesLink()}
        {!organizationsEnabled && this.renderRulesLink()}
        {!organizationsEnabled && this.renderProfilesLink()}
        {!organizationsEnabled && this.renderQualityGatesLink()}
        {this.renderAdministrationLink()}
        {this.renderMore()}
      </ul>
    );
  }
}
