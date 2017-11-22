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
import * as React from 'react';
import { connect } from 'react-redux';
import { Link, IndexLink } from 'react-router';
import * as classNames from 'classnames';
import { Organization, Extension } from '../../app/types';
import ContextNavBar from '../../components/nav/ContextNavBar';
import NavBarTabs from '../../components/nav/NavBarTabs';
import OrganizationIcon from '../../components/icons-components/OrganizationIcon';
import CommonNavigation from '../../components/common/CommonNavigation';
import { AppState } from '../../store/appState/duck';
import { getAppState } from '../../store/rootReducer';
import { translate } from '../../helpers/l10n';

const PORTFOLIOS = 'governance/portfolios';
const SUPPORT = 'license/support';

interface Props {
  appState: AppState;
  location: { pathname: string };
  organization: Organization;
}

function DefaultOrganizationNavigation({ appState, location, organization }: Props) {
  const portfoliosExtension = appState.globalPages.find(({ key }) => key === PORTFOLIOS);
  const portfoliosLink = portfoliosExtension && (
    <li key="portfolios">
      <Link to="/portfolios" activeClassName="active">
        {translate('portfolios.page')}
      </Link>
    </li>
  );

  return (
    <ContextNavBar id="global-navigation" height={65}>
      {renderHeader(organization)}
      {renderMeta(organization)}
      <NavBarTabs>
        <CommonNavigation afterProjects={portfoliosLink} />
        {renderExtensions(appState.globalPages, location.pathname)}
        {organization.canAdmin && renderAdministration(appState.adminPages, location.pathname)}
      </NavBarTabs>
    </ContextNavBar>
  );
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state)
});

export default connect(mapStateToProps)(DefaultOrganizationNavigation);

function renderHeader(organization: Organization) {
  return (
    <div className="navbar-context-header">
      <h1 className="display-inline-block">
        <OrganizationIcon className="little-spacer-right" />
        <Link to="/projects" className="link-base-color link-no-underline">
          <strong>{organization.name}</strong>
        </Link>
      </h1>
      {organization.description != null && (
        <div className="navbar-context-description">
          <p className="text-limited text-top" title={organization.description}>
            {organization.description}
          </p>
        </div>
      )}
    </div>
  );
}

function renderMeta(organization: Organization) {
  return (
    <div className="navbar-context-meta">
      {!!organization.avatar && (
        <img src={organization.avatar} height={30} alt={organization.name} />
      )}
      {organization.url != null && (
        <div>
          <p className="text-limited text-top">
            <a
              className="link-underline"
              href={organization.url}
              title={organization.url}
              rel="nofollow">
              {organization.url}
            </a>
          </p>
        </div>
      )}
    </div>
  );
}

function renderExtensions(extensions: Extension[], pathname: string) {
  const withoutPortfolios = extensions.filter(({ key }) => key !== PORTFOLIOS);
  if (withoutPortfolios.length === 0) {
    return null;
  }
  const isDropdownActive = withoutPortfolios.some(({ key }) => pathname === `extension/${key}`);
  return (
    <li className="dropdown">
      <a
        className={classNames('dropdown-toggle', { active: isDropdownActive })}
        data-toggle="dropdown"
        id="global-navigation-more"
        href="#">
        {translate('more')}
        <span className="icon-dropdown little-spacer-left" />
      </a>
      <ul className="dropdown-menu">
        {withoutPortfolios.map(({ key, name }) => (
          <li key={key}>
            <Link to={`/extension/${key}`} activeClassName="active">
              {name}
            </Link>
          </li>
        ))}
      </ul>
    </li>
  );
}

function renderAdministration(extensions: Extension[] = [], pathname: string) {
  const supportExtension = extensions.find(({ key }) => key === SUPPORT);
  const withoutSupport = extensions.filter(({ key }) => key !== SUPPORT);
  const adminActive = pathname.startsWith('admin');

  return (
    <li className="dropdown">
      <a
        className={classNames('dropdown-toggle', { active: adminActive })}
        data-toggle="dropdown"
        href="#">
        {translate('layout.settings')}
        <i className="icon-dropdown little-spacer-left" />
      </a>
      <ul className="dropdown-menu">
        <li className="dropdown-header">{translate('sidebar.project_settings')}</li>
        {adminLink('/admin/settings', 'settings.page')}
        {adminLink('/admin/settings/encryption', 'property.category.security.encryption')}
        {adminLink('/admin/custom_metrics', 'custom_metrics.page')}
        {withoutSupport.map(({ key, name }) => (
          <li key={key}>
            <Link to={`/admin/extension/${key}`} activeClassName="active">
              {name}
            </Link>
          </li>
        ))}

        <li className="divider" />
        <li className="dropdown-header">{translate('sidebar.security')}</li>
        {adminLink('/admin/users', 'users.page')}
        {adminLink('/admin/groups', 'user_groups.page')}
        {adminLink('/admin/permissions', 'global_permissions.page')}
        {adminLink('/admin/permission_templates', 'permission_templates')}

        <li className="divider" />
        {adminLink('/admin/projects_management', 'projects_management')}
        {adminLink('/admin/background_tasks', 'background_tasks.page')}

        <li className="divider" />
        {adminLink('/admin/system', 'sidebar.system')}
        {adminLink('/admin/marketplace', 'marketplace.page')}
        {supportExtension && adminLink('/admin/extension/license/support', 'support')}
      </ul>
    </li>
  );
}

function adminLink(path: string, label: string) {
  return (
    <li>
      <IndexLink to={path} activeClassName="active">
        {translate(label)}
      </IndexLink>
    </li>
  );
}
