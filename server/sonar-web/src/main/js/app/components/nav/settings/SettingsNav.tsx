/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { DropdownMenu, DropdownMenuAlign } from '@sonarsource/echoes-react';
import * as React from 'react';
import { Location } from 'react-router-dom';
import { LightLabel, NavBarTabLink, NavBarTabs, TopBar } from '~design-system';
import withLocation from '../../../../components/hoc/withLocation';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { AdminPageExtension } from '../../../../types/extension';
import { PendingPluginResult } from '../../../../types/plugins';
import { Extension, SysStatus } from '../../../../types/types';
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
      <DropdownMenu.ItemLink isMatchingFullPath key={key} to={`/admin/extension/${key}`}>
        {name}
      </DropdownMenu.ItemLink>
    );
  };

  renderConfigurationTab() {
    const { canAdmin } = this.props;
    const extensionsWithoutSupport = this.props.extensions.filter(
      (extension) => extension.key !== 'license/support',
    );

    return (
      <DropdownMenu.Root
        align={DropdownMenuAlign.Start}
        id="settings-navigation-configuration-dropdown"
        items={
          <>
            <DropdownMenu.ItemLink isMatchingFullPath to="/admin/settings">
              {translate('settings.page')}
            </DropdownMenu.ItemLink>

            {canAdmin && (
              <>
                <DropdownMenu.ItemLink isMatchingFullPath to="/admin/settings/encryption">
                  {translate('property.category.security.encryption')}
                </DropdownMenu.ItemLink>
              </>
            )}

            {extensionsWithoutSupport.map(this.renderExtension)}
          </>
        }
      >
        <NavBarTabLink
          aria-haspopup="menu"
          active={
            !this.isSecurityActive() &&
            !this.isProjectsActive() &&
            !this.isSystemActive() &&
            !this.isSomethingActive(['/admin/extension/license/support']) &&
            !this.isMarketplace() &&
            !this.isAudit()
          }
          id="settings-navigation-configuration"
          text={translate('sidebar.project_settings')}
          to={{}}
          withChevron
        />
      </DropdownMenu.Root>
    );
  }

  renderProjectsTab() {
    return (
      <DropdownMenu.Root
        id="settings-navigation-projects-dropdown"
        items={
          <>
            <DropdownMenu.ItemLink isMatchingFullPath to="/admin/background_tasks">
              {translate('background_tasks.page')}
            </DropdownMenu.ItemLink>
          </>
        }
      >
        <NavBarTabLink
          aria-haspopup="menu"
          active={this.isProjectsActive()}
          to={{}}
          text={translate('sidebar.projects')}
          withChevron
        />
      </DropdownMenu.Root>
    );
  }

  renderSecurityTab() {
    return (
      <DropdownMenu.Root
        id="settings-navigation-security-dropdown"
        items={
          <>
            <DropdownMenu.ItemLink isMatchingFullPath to="/admin/users">
              {translate('users.page')}
            </DropdownMenu.ItemLink>
          </>
        }
      >
        <NavBarTabLink
          aria-haspopup="menu"
          active={this.isSecurityActive()}
          to={{}}
          text={translate('sidebar.security')}
          withChevron
        />
      </DropdownMenu.Root>
    );
  }

  render() {
    const { extensions, pendingPlugins, canAdmin, canCustomerAdmin } = this.props;
    const hasSupportExtension = extensions.find((extension) => extension.key === 'license/support');

    const hasGovernanceExtension = extensions.find(
      (e) => e.key === AdminPageExtension.GovernanceConsole,
    );

    const totalPendingPlugins =
      pendingPlugins.installing.length +
      pendingPlugins.removing.length +
      pendingPlugins.updating.length;

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
      <>
        <TopBar id="context-navigation" aria-label={translate('settings')}>
          <LightLabel as="h1">{translate('layout.settings')}</LightLabel>

          <NavBarTabs className="it__navbar-tabs sw-mt-4">
            {(canAdmin || canCustomerAdmin) && this.renderConfigurationTab()}
            {canAdmin && this.renderSecurityTab()}
            {(canAdmin || canCustomerAdmin) && this.renderProjectsTab()}

            {canAdmin && (
              <>
                <NavBarTabLink end to="/admin/system" text={translate('sidebar.system')} />

                <NavBarTabLink end to="/admin/marketplace" text={translate('marketplace.page')} />
              </>
            )}

            {hasGovernanceExtension && (
              <NavBarTabLink end to="/admin/audit" text={translate('audit_logs.page')} />
            )}

            {hasSupportExtension && (
              <NavBarTabLink
                end
                to="/admin/extension/license/support"
                text={translate('support')}
              />
            )}
          </NavBarTabs>
        </TopBar>

        {notifComponent}
      </>
    );
  }
}

export default withLocation(SettingsNav);
