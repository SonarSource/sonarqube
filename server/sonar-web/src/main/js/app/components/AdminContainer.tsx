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
import * as React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getSettingsNavigation } from '../../api/nav';
import { getPendingPlugins, PluginPendingResult } from '../../api/plugins';
import { getSystemStatus, waitSystemUPStatus } from '../../api/system';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import { setAdminPages } from '../../store/appState';
import { getAppState, Store } from '../../store/rootReducer';
import AdminContext, { defaultPendingPlugins, defaultSystemStatus } from './AdminContext';
import SettingsNav from './nav/settings/SettingsNav';

interface Props {
  appState: Pick<T.AppState, 'adminPages' | 'canAdmin' | 'organizationsEnabled'>;
  location: {};
  setAdminPages: (adminPages: T.Extension[]) => void;
}

interface State {
  pendingPlugins: PluginPendingResult;
  systemStatus: T.SysStatus;
}

export class AdminContainer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    pendingPlugins: defaultPendingPlugins,
    systemStatus: defaultSystemStatus
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
    getSettingsNavigation().then(r => this.props.setAdminPages(r.extensions), () => {});
  };

  fetchPendingPlugins = () => {
    getPendingPlugins().then(
      pendingPlugins => {
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
    const { adminPages, organizationsEnabled } = this.props.appState;

    // Check that the adminPages are loaded
    if (!adminPages) {
      return null;
    }

    const { pendingPlugins, systemStatus } = this.state;
    const defaultTitle = translate('layout.settings');

    return (
      <div>
        <Helmet defaultTitle={defaultTitle} titleTemplate={'%s - ' + defaultTitle} />
        <SettingsNav
          extensions={adminPages}
          fetchPendingPlugins={this.fetchPendingPlugins}
          fetchSystemStatus={this.fetchSystemStatus}
          location={this.props.location}
          organizationsEnabled={organizationsEnabled}
          pendingPlugins={pendingPlugins}
          systemStatus={systemStatus}
        />
        <AdminContext.Provider
          value={{
            fetchSystemStatus: this.fetchSystemStatus,
            fetchPendingPlugins: this.fetchPendingPlugins,
            pendingPlugins,
            systemStatus
          }}>
          {this.props.children}
        </AdminContext.Provider>
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({ appState: getAppState(state) });

const mapDispatchToProps = { setAdminPages };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AdminContainer);
