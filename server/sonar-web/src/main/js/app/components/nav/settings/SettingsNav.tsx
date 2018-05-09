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
import PendingPluginsActionNotif from './PendingPluginsActionNotif';
import * as theme from '../../../../app/theme';
import ContextNavBar from '../../../../components/nav/ContextNavBar';
import NavBarTabs from '../../../../components/nav/NavBarTabs';
import { EditionStatus } from '../../../../api/marketplace';
import { Extension } from '../../../types';
import { translate } from '../../../../helpers/l10n';
import Dropdown from '../../../../components/controls/Dropdown';
import { PluginPendingResult } from '../../../../api/plugins';

interface Props {
  editionStatus?: EditionStatus;
  extensions: Extension[];
  fetchPendingPlugins: () => void;
  location: {};
  organizationsEnabled: boolean;
  pendingPlugins: PluginPendingResult;
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
        <Link activeClassName="active" to={`/admin/extension/${key}`}>
          {name}
        </Link>
      </li>
    );
  };

  renderConfigurationTab() {
    const { organizationsEnabled } = this.props;
    const extensionsWithoutSupport = this.props.extensions.filter(
      extension => extension.key !== 'license/support'
    );
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <IndexLink activeClassName="active" to="/admin/settings">
                {translate('settings.page')}
              </IndexLink>
            </li>
            <li>
              <IndexLink activeClassName="active" to="/admin/settings/encryption">
                {translate('property.category.security.encryption')}
              </IndexLink>
            </li>
            <li>
              <IndexLink activeClassName="active" to="/admin/custom_metrics">
                {translate('custom_metrics.page')}
              </IndexLink>
            </li>
            {!organizationsEnabled && (
              <li>
                <IndexLink activeClassName="active" to="/admin/webhooks">
                  {translate('webhooks.page')}
                </IndexLink>
              </li>
            )}
            {extensionsWithoutSupport.map(this.renderExtension)}
          </ul>
        }
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={String(open)}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', {
              active:
                open ||
                (!this.isSecurityActive() &&
                  !this.isProjectsActive() &&
                  !this.isSystemActive() &&
                  !this.isSomethingActive(['/admin/extension/license/support']) &&
                  !this.isMarketplace())
            })}
            href="#"
            id="settings-navigation-configuration"
            onClick={onToggleClick}>
            {translate('sidebar.project_settings')}
            <i className="icon-dropdown little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderProjectsTab() {
    const { organizationsEnabled } = this.props;
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            {!organizationsEnabled && (
              <li>
                <IndexLink activeClassName="active" to="/admin/projects_management">
                  {translate('management')}
                </IndexLink>
              </li>
            )}
            <li>
              <IndexLink activeClassName="active" to="/admin/background_tasks">
                {translate('background_tasks.page')}
              </IndexLink>
            </li>
          </ul>
        }
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={String(open)}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: open || this.isProjectsActive() })}
            href="#"
            onClick={onToggleClick}>
            {translate('sidebar.projects')} <i className="icon-dropdown" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderSecurityTab() {
    const { organizationsEnabled } = this.props;
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <IndexLink activeClassName="active" to="/admin/users">
                {translate('users.page')}
              </IndexLink>
            </li>
            {!organizationsEnabled && (
              <li>
                <IndexLink activeClassName="active" to="/admin/groups">
                  {translate('user_groups.page')}
                </IndexLink>
              </li>
            )}
            {!organizationsEnabled && (
              <li>
                <IndexLink activeClassName="active" to="/admin/permissions">
                  {translate('global_permissions.page')}
                </IndexLink>
              </li>
            )}
            {!organizationsEnabled && (
              <li>
                <IndexLink activeClassName="active" to="/admin/permission_templates">
                  {translate('permission_templates')}
                </IndexLink>
              </li>
            )}
          </ul>
        }
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={String(open)}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: open || this.isSecurityActive() })}
            href="#"
            onClick={onToggleClick}>
            {translate('sidebar.security')} <i className="icon-dropdown" />
          </a>
        )}
      </Dropdown>
    );
  }

  render() {
    const { editionStatus, extensions, pendingPlugins } = this.props;
    const hasSupportExtension = extensions.find(extension => extension.key === 'license/support');

    const notifComponents = [];
    if (
      editionStatus &&
      (editionStatus.installError || editionStatus.installationStatus !== 'NONE')
    ) {
      notifComponents.push(<SettingsEditionsNotifContainer editionStatus={editionStatus} />);
    }

    if (
      pendingPlugins.installing.length > 0 ||
      pendingPlugins.removing.length > 0 ||
      pendingPlugins.updating.length > 0
    ) {
      notifComponents.push(
        <PendingPluginsActionNotif
          pending={pendingPlugins}
          refreshPending={this.props.fetchPendingPlugins}
        />
      );
    }

    const notifContainer =
      notifComponents.length > 0 ? (
        <div className="alert-container">
          {notifComponents.map((element, index) => <div key={index}>{element}</div>)}
        </div>
      ) : null;

    return (
      <ContextNavBar
        height={theme.contextNavHeightRaw + 38 * notifComponents.length}
        id="context-navigation"
        notif={notifContainer}>
        <header className="navbar-context-header">
          <h1>{translate('layout.settings')}</h1>
        </header>

        <NavBarTabs>
          {this.renderConfigurationTab()}
          {this.renderSecurityTab()}
          {this.renderProjectsTab()}

          <li>
            <IndexLink activeClassName="active" to="/admin/system">
              {translate('sidebar.system')}
            </IndexLink>
          </li>

          <li>
            <IndexLink activeClassName="active" to="/admin/marketplace">
              {translate('marketplace.page')}
            </IndexLink>
          </li>

          {hasSupportExtension && (
            <li>
              <IndexLink activeClassName="active" to="/admin/extension/license/support">
                {translate('support')}
              </IndexLink>
            </li>
          )}
        </NavBarTabs>
      </ContextNavBar>
    );
  }
}
