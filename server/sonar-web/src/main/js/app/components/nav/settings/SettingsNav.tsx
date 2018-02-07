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
import * as classNames from 'classnames';
import { IndexLink, Link } from 'react-router';
import SettingsEditionsNotifContainer from './SettingsEditionsNotifContainer';
import * as theme from '../../../../app/theme';
import ContextNavBar from '../../../../components/nav/ContextNavBar';
import NavBarTabs from '../../../../components/nav/NavBarTabs';
import { EditionStatus } from '../../../../api/marketplace';
import { Extension } from '../../../types';
import { translate } from '../../../../helpers/l10n';

interface Props {
  editionStatus?: EditionStatus;
  extensions: Extension[];
  customOrganizations: boolean;
  location: {};
}

export default class SettingsNav extends React.PureComponent<Props> {
  static defaultProps = {
    extensions: []
  };

  isSomethingActive(urls: string[]): boolean {
    const path = window.location.pathname;
    return urls.some((url: string) => path.indexOf((window as any).baseUrl + url) === 0);
  }

  isSecurityActive() {
    const urls = [
      '/admin/users',
      '/admin/groups',
      '/admin/permissions',
      '/admin/permission_templates'
    ];
    return this.isSomethingActive(urls);
  }

  isProjectsActive() {
    const urls = ['/admin/projects_management', '/admin/background_tasks'];
    return this.isSomethingActive(urls);
  }

  isSystemActive() {
    const urls = ['/admin/system'];
    return this.isSomethingActive(urls);
  }

  isMarketplace() {
    const urls = ['/admin/marketplace'];
    return this.isSomethingActive(urls);
  }

  renderExtension = ({ key, name }: Extension) => {
    return (
      <li key={key}>
        <Link to={`/admin/extension/${key}`} activeClassName="active">
          {name}
        </Link>
      </li>
    );
  };

  renderConfigurationTab() {
    const configurationClassNames = classNames('dropdown-toggle', {
      active:
        !this.isSecurityActive() &&
        !this.isProjectsActive() &&
        !this.isSystemActive() &&
        !this.isSomethingActive(['/admin/extension/license/support']) &&
        !this.isMarketplace()
    });
    const extensionsWithoutSupport = this.props.extensions.filter(
      extension => extension.key !== 'license/support'
    );
    return (
      <li className="dropdown">
        <a
          className={configurationClassNames}
          data-toggle="dropdown"
          id="settings-navigation-configuration"
          href="#">
          {translate('sidebar.project_settings')} <i className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu">
          <li>
            <IndexLink to="/admin/settings" activeClassName="active">
              {translate('settings.page')}
            </IndexLink>
          </li>
          <li>
            <IndexLink to="/admin/settings/encryption" activeClassName="active">
              {translate('property.category.security.encryption')}
            </IndexLink>
          </li>
          <li>
            <IndexLink to="/admin/custom_metrics" activeClassName="active">
              {translate('custom_metrics.page')}
            </IndexLink>
          </li>
          {extensionsWithoutSupport.map(this.renderExtension)}
        </ul>
      </li>
    );
  }

  renderProjectsTab() {
    const { customOrganizations } = this.props;
    const projectsClassName = classNames('dropdown-toggle', { active: this.isProjectsActive() });
    return (
      <li className="dropdown">
        <a className={projectsClassName} data-toggle="dropdown" href="#">
          {translate('sidebar.projects')} <i className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu">
          {!customOrganizations && (
            <li>
              <IndexLink to="/admin/projects_management" activeClassName="active">
                {translate('management')}
              </IndexLink>
            </li>
          )}
          <li>
            <IndexLink to="/admin/background_tasks" activeClassName="active">
              {translate('background_tasks.page')}
            </IndexLink>
          </li>
        </ul>
      </li>
    );
  }

  renderSecurityTab() {
    const { customOrganizations } = this.props;
    const securityClassName = classNames('dropdown-toggle', { active: this.isSecurityActive() });
    return (
      <li className="dropdown">
        <a className={securityClassName} data-toggle="dropdown" href="#">
          {translate('sidebar.security')} <i className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu">
          <li>
            <IndexLink to="/admin/users" activeClassName="active">
              {translate('users.page')}
            </IndexLink>
          </li>
          {!customOrganizations && (
            <li>
              <IndexLink to="/admin/groups" activeClassName="active">
                {translate('user_groups.page')}
              </IndexLink>
            </li>
          )}
          {!customOrganizations && (
            <li>
              <IndexLink to="/admin/permissions" activeClassName="active">
                {translate('global_permissions.page')}
              </IndexLink>
            </li>
          )}
          {!customOrganizations && (
            <li>
              <IndexLink to="/admin/permission_templates" activeClassName="active">
                {translate('permission_templates')}
              </IndexLink>
            </li>
          )}
        </ul>
      </li>
    );
  }

  render() {
    const { editionStatus, extensions } = this.props;
    const hasSupportExtension = extensions.find(extension => extension.key === 'license/support');

    let notifComponent;
    if (
      editionStatus &&
      (editionStatus.installError || editionStatus.installationStatus !== 'NONE')
    ) {
      notifComponent = <SettingsEditionsNotifContainer editionStatus={editionStatus} />;
    }

    return (
      <ContextNavBar
        id="context-navigation"
        height={notifComponent ? theme.contextNavHeightRaw + 20 : theme.contextNavHeightRaw}
        notif={notifComponent}>
        <header className="navbar-context-header">
          <h1>{translate('layout.settings')}</h1>
        </header>

        <NavBarTabs>
          {this.renderConfigurationTab()}
          {this.renderSecurityTab()}
          {this.renderProjectsTab()}

          <li>
            <IndexLink to="/admin/system" activeClassName="active">
              {translate('sidebar.system')}
            </IndexLink>
          </li>

          <li>
            <IndexLink to="/admin/marketplace" activeClassName="active">
              {translate('marketplace.page')}
            </IndexLink>
          </li>

          {hasSupportExtension && (
            <li>
              <IndexLink to="/admin/extension/license/support" activeClassName="active">
                {translate('support')}
              </IndexLink>
            </li>
          )}
        </NavBarTabs>
      </ContextNavBar>
    );
  }
}
