/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isMySet } from '../../../../apps/issues/utils';
import { isSonarCloud } from '../../../../helpers/system';
import { getQualityGatesUrl } from '../../../../helpers/urls';
import { isLoggedIn } from '../../../../helpers/users';

interface Props {
  appState: Pick<T.AppState, 'canAdmin' | 'globalPages' | 'organizationsEnabled' | 'qualifiers'>;
  currentUser: T.CurrentUser;
  location: { pathname: string };
}

export default class GlobalNavMenu extends React.PureComponent<Props> {
  renderProjects() {
    if (isSonarCloud() && !isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const active =
      this.props.location.pathname.startsWith('/projects') &&
      this.props.location.pathname !== '/projects/create';

    return (
      <li>
        <Link className={classNames({ active })} to="/projects">
          {isSonarCloud() ? translate('my_projects') : translate('projects.page')}
        </Link>
      </li>
    );
  }

  renderPortfolios() {
    return (
      <li>
        <Link activeClassName="active" to="/portfolios">
          {translate('portfolios.page')}
        </Link>
      </li>
    );
  }

  renderIssuesLink() {
    if (isSonarCloud() && !isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const active = this.props.location.pathname.startsWith('/issues');

    if (isSonarCloud()) {
      return (
        <li>
          <Link
            className={classNames({ active })}
            to={{ pathname: '/issues', query: { resolved: 'false' } }}>
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
        <Link className={classNames({ active })} to={{ pathname: '/issues', query }}>
          {translate('issues.page')}
        </Link>
      </li>
    );
  }

  renderRulesLink() {
    return (
      <li>
        <Link activeClassName="active" to="/coding_rules">
          {translate('coding_rules.page')}
        </Link>
      </li>
    );
  }

  renderProfilesLink() {
    return (
      <li>
        <Link activeClassName="active" to="/profiles">
          {translate('quality_profiles.page')}
        </Link>
      </li>
    );
  }

  renderQualityGatesLink() {
    return (
      <li>
        <Link activeClassName="active" to={getQualityGatesUrl()}>
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
        <Link activeClassName="active" to="/admin">
          {translate('layout.settings')}
        </Link>
      </li>
    );
  }

  renderGlobalPageLink = ({ key, name }: T.Extension) => {
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
      <Dropdown
        overlay={<ul className="menu">{withoutPortfolios.map(this.renderGlobalPageLink)}</ul>}
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: open })}
            href="#"
            id="global-navigation-more"
            onClick={onToggleClick}>
            {translate('more')}
            <DropdownIcon className="little-spacer-left text-middle" />
          </a>
        )}
      </Dropdown>
    );
  }

  render() {
    const governanceInstalled = this.props.appState.qualifiers.includes('VW');
    const { organizationsEnabled } = this.props.appState;

    return (
      <ul className="global-navbar-menu">
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
