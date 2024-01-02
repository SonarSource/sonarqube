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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Outlet } from 'react-router-dom';
import { getSettingsNavigation } from '../../api/navigation';
import { getPendingPlugins } from '../../api/plugins';
import { getSystemStatus, waitSystemUPStatus } from '../../api/system';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import { translate } from '../../helpers/l10n';
import { AdminPagesContext } from '../../types/admin';
import { AppState } from '../../types/appstate';
import { PendingPluginResult } from '../../types/plugins';
import { Extension, SysStatus } from '../../types/types';
import AdminContext, { defaultPendingPlugins, defaultSystemStatus } from './AdminContext';
import withAppStateContext from './app-state/withAppStateContext';
import SettingsNav from './nav/settings/SettingsNav';

export interface AdminContainerProps {
  appState: AppState;
}

interface State {
  pendingPlugins: PendingPluginResult;
  systemStatus: SysStatus;
  adminPages: Extension[];
}

export class AdminContainer extends React.PureComponent<AdminContainerProps, State> {
  mounted = false;
  state: State = {
    pendingPlugins: defaultPendingPlugins,
    systemStatus: defaultSystemStatus,
    adminPages: [],
  };

  componentDidMount() {
    this.mounted = true;
    if (!this.props.appState.canAdmin) {
      handleRequiredAuthorization();
    } else {
      this.fetchNavigationSettings();
      this.fetchPendingPlugins();
      this.fetchSystemStatus();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchNavigationSettings = () => {
    getSettingsNavigation().then(
      (r) => this.setState({ adminPages: r.extensions }),
      () => {}
    );
  };

  fetchPendingPlugins = () => {
    getPendingPlugins().then(
      (pendingPlugins) => {
        if (this.mounted) {
          this.setState({ pendingPlugins });
        }
      },
      () => {}
    );
  };

  fetchSystemStatus = () => {
    getSystemStatus().then(
      ({ status }) => {
        if (this.mounted) {
          this.setState({ systemStatus: status });
          if (status === 'RESTARTING') {
            this.waitRestartingDone();
          }
        }
      },
      () => {}
    );
  };

  waitRestartingDone = () => {
    waitSystemUPStatus().then(
      ({ status }) => {
        if (this.mounted) {
          this.setState({ systemStatus: status });
          document.location.reload();
        }
      },
      () => {}
    );
  };

  render() {
    const { adminPages } = this.state;

    // Check that the adminPages are loaded
    if (!adminPages) {
      return null;
    }

    const { pendingPlugins, systemStatus } = this.state;
    const defaultTitle = translate('layout.settings');

    const adminPagesContext: AdminPagesContext = { adminPages };

    return (
      <div>
        <Helmet defaultTitle={defaultTitle} defer={false} titleTemplate={`%s - ${defaultTitle}`} />
        <SettingsNav
          extensions={adminPages}
          fetchPendingPlugins={this.fetchPendingPlugins}
          fetchSystemStatus={this.fetchSystemStatus}
          pendingPlugins={pendingPlugins}
          systemStatus={systemStatus}
        />
        <AdminContext.Provider
          value={{
            fetchSystemStatus: this.fetchSystemStatus,
            fetchPendingPlugins: this.fetchPendingPlugins,
            pendingPlugins,
            systemStatus,
          }}
        >
          <Outlet context={adminPagesContext} />
        </AdminContext.Provider>
      </div>
    );
  }
}

export default withAppStateContext(AdminContainer);
