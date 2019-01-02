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
import MarketplaceContext, { defaultPendingPlugins } from './MarketplaceContext';
import SettingsNav from './nav/settings/SettingsNav';
import { getAppState, Store } from '../../store/rootReducer';
import { getSettingsNavigation } from '../../api/nav';
import { setAdminPages } from '../../store/appState';
import { translate } from '../../helpers/l10n';
import { PluginPendingResult, getPendingPlugins } from '../../api/plugins';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';

interface StateProps {
  appState: Pick<T.AppState, 'adminPages' | 'canAdmin' | 'organizationsEnabled'>;
}

interface DispatchToProps {
  setAdminPages: (adminPages: T.Extension[]) => void;
}

interface OwnProps {
  location: {};
}

type Props = StateProps & DispatchToProps & OwnProps;

interface State {
  pendingPlugins: PluginPendingResult;
}

class AdminContainer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    pendingPlugins: defaultPendingPlugins
  };

  componentDidMount() {
    this.mounted = true;
    if (!this.props.appState.canAdmin) {
      handleRequiredAuthorization();
    } else {
      this.fetchNavigationSettings();
      this.fetchPendingPlugins();
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

  render() {
    const { adminPages, organizationsEnabled } = this.props.appState;

    // Check that the adminPages are loaded
    if (!adminPages) {
      return null;
    }

    const defaultTitle = translate('layout.settings');

    return (
      <div>
        <Helmet defaultTitle={defaultTitle} titleTemplate={'%s - ' + defaultTitle} />
        <SettingsNav
          extensions={adminPages}
          fetchPendingPlugins={this.fetchPendingPlugins}
          location={this.props.location}
          organizationsEnabled={organizationsEnabled}
          pendingPlugins={this.state.pendingPlugins}
        />
        <MarketplaceContext.Provider
          value={{
            fetchPendingPlugins: this.fetchPendingPlugins,
            pendingPlugins: this.state.pendingPlugins
          }}>
          {this.props.children}
        </MarketplaceContext.Provider>
      </div>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  appState: getAppState(state)
});

const mapDispatchToProps: DispatchToProps = {
  setAdminPages
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AdminContainer);
