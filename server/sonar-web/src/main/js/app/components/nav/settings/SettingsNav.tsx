/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { Location, NavLink } from 'react-router-dom';
import Dropdown from '../../../../components/controls/Dropdown';
import withLocation from '../../../../components/hoc/withLocation';
import DropdownIcon from '../../../../components/icons/DropdownIcon';
import ContextNavBar from '../../../../components/ui/ContextNavBar';
import NavBarTabs from '../../../../components/ui/NavBarTabs';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { AdminPageExtension } from '../../../../types/extension';
import { PendingPluginResult } from '../../../../types/plugins';
import { Extension, SysStatus } from '../../../../types/types';
import { rawSizes } from '../../../theme';
import PendingPluginsActionNotif from './PendingPluginsActionNotif';
import SystemRestartNotif from './SystemRestartNotif';

interface Props {
  extensions: Extension[];
  fetchPendingPlugins: () => void;
  fetchSystemStatus: () => void;
  location: Location;
  pendingPlugins: PendingPluginResult;
  systemStatus: SysStatus;
  canAdmin?: boolean;
  canCustomerAdmin?: boolean;
}

const ALERT_HEIGHT = 30;

export class SettingsNav extends React.PureComponent<Props> {
  static defaultProps = {
    extensions: [],
  };

  isSomethingActive = (urls: string[]) => {
    const path = this.props.location.pathname;
    return urls.some((url: string) => path.indexOf(getBaseUrl() + url) === 0);
  };

  isSecurityActive() {
    const urls = [
      '/admin/users',
      '/admin/groups',
      '/admin/permissions',
      '/admin/permission_templates',
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

  isAudit() {
    const urls = ['/admin/audit'];
    return this.isSomethingActive(urls);
  }

  renderExtension = ({ key, name }: Extension) => {
    return (
      <li key={key}>
        <NavLink to={`/admin/extension/${key}`}>{name}</NavLink>
      </li>
    );
  };

  renderConfigurationTab() {
    const { canAdmin } = this.props;
    const extensionsWithoutSupport = this.props.extensions.filter(
      (extension) => extension.key !== 'license/support'
    );
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <NavLink end={true} to={canAdmin ? "/admin/settings" : "/admin/settings?category=codescan"}>
                {translate('settings.page')}
              </NavLink>
            </li>
            {canAdmin && (
              <>
                <li>
                  <NavLink end={true} to="/admin/settings/encryption">
                    {translate('property.category.security.encryption')}
                  </NavLink>
                </li>
                <li>
                  <NavLink activeClassName="active" to="/admin/custom_metrics">
                    {translate('custom_metrics.page')}
                  </NavLink>
                </li>
                <li>
                  <NavLink end={true} to="/admin/webhooks">
                    {translate('webhooks.page')}
                  </NavLink>
                </li>
              </>
            )}
            {extensionsWithoutSupport.map(this.renderExtension)}
          </ul>
        }
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="menu"
            role="button"
            className={classNames('dropdown-toggle', {
              active:
                open ||
                (!this.isSecurityActive() &&
                  !this.isProjectsActive() &&
                  !this.isSystemActive() &&
                  !this.isSomethingActive(['/admin/extension/license/support']) &&
                  !this.isMarketplace() &&
                  !this.isAudit()),
            })}
            href="#"
            id="settings-navigation-configuration"
            onClick={onToggleClick}
          >
            {translate('sidebar.project_settings')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderProjectsTab() {
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <NavLink end={true} to="/admin/projects_management">
                {translate('management')}
              </NavLink>
            </li>
            <li>
              <NavLink end={true} to="/admin/background_tasks">
                {translate('background_tasks.page')}
              </NavLink>
            </li>
          </ul>
        }
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="menu"
            role="button"
            className={classNames('dropdown-toggle', { active: open || this.isProjectsActive() })}
            href="#"
            onClick={onToggleClick}
          >
            {translate('sidebar.projects')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderSecurityTab() {
    return (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <NavLink end={true} to="/admin/users">
                {translate('users.page')}
              </NavLink>
            </li>
            <li>
              <NavLink end={true} to="/admin/groups">
                {translate('user_groups.page')}
              </NavLink>
            </li>
            <li>
              <NavLink end={true} to="/admin/permissions">
                {translate('global_permissions.page')}
              </NavLink>
            </li>
            <li>
              <NavLink end={true} to="/admin/permission_templates">
                {translate('permission_templates')}
              </NavLink>
            </li>
          </ul>
        }
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="menu"
            role="button"
            className={classNames('dropdown-toggle', { active: open || this.isSecurityActive() })}
            href="#"
            onClick={onToggleClick}
          >
            {translate('sidebar.security')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  render() {
    const { extensions, pendingPlugins, canAdmin, canCustomerAdmin } = this.props;
    const hasSupportExtension = extensions.find((extension) => extension.key === 'license/support');
    const hasGovernanceExtension = extensions.find(
      (e) => e.key === AdminPageExtension.GovernanceConsole
    );
    const totalPendingPlugins =
      pendingPlugins.installing.length +
      pendingPlugins.removing.length +
      pendingPlugins.updating.length;
    const contextNavHeight = rawSizes.contextNavHeightRaw;
    let notifComponent;
    if (this.props.systemStatus === 'RESTARTING') {
      notifComponent = <SystemRestartNotif />;
    } else if (totalPendingPlugins > 0) {
      notifComponent = (
        <PendingPluginsActionNotif
          fetchSystemStatus={this.props.fetchSystemStatus}
          pending={pendingPlugins}
          refreshPending={this.props.fetchPendingPlugins}
          systemStatus={this.props.systemStatus}
        />
      );
    }

    return (
      <ContextNavBar
        height={notifComponent ? contextNavHeight + ALERT_HEIGHT : contextNavHeight}
        id="context-navigation"
        notif={notifComponent}
      >
        <div className="navbar-context-header">
          <h1>{translate('layout.settings')}</h1>
        </div>

        <NavBarTabs>
          {(canAdmin || canCustomerAdmin) && this.renderConfigurationTab()}
          {canAdmin && this.renderSecurityTab()}
          {(canAdmin || canCustomerAdmin) && this.renderProjectsTab()}

          {canAdmin &&
            (<li>
              <IndexLink activeClassName="active" to="/admin/system">
                {translate('sidebar.system')}
              </IndexLink>
            </li>)}

          {canAdmin &&
            (<li>
              <IndexLink activeClassName="active" to="/admin/marketplace">
                {translate('marketplace.page')}
              </IndexLink>
            </li>)}

          {hasGovernanceExtension && (
            <li>
              <NavLink end={true} to="/admin/audit">
                {translate('audit_logs.page')}
              </NavLink>
            </li>
          )}

          {canAdmin && hasSupportExtension && (
            <li>
              <NavLink end={true} to="/admin/extension/license/support">
                {translate('support')}
              </NavLink>
            </li>
          )}
        </NavBarTabs>
      </ContextNavBar>
    );
  }
}

export default withLocation(SettingsNav);
