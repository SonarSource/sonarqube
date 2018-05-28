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
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import SettingsNav from './nav/settings/SettingsNav';
import { getAppState, getMarketplacePendingPlugins } from '../../store/rootReducer';
import { getSettingsNavigation } from '../../api/nav';
import { setAdminPages } from '../../store/appState/duck';
import { fetchCurrentEdition, fetchPendingPlugins } from '../../store/marketplace/actions';
import { translate } from '../../helpers/l10n';
import { Extension } from '../types';
import { PluginPendingResult } from '../../api/plugins';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';

interface StateProps {
  appState: {
    adminPages: Extension[];
    organizationsEnabled: boolean;
    version: string;
  };
  pendingPlugins: PluginPendingResult;
}

interface DispatchToProps {
  fetchPendingPlugins: () => void;
  fetchCurrentEdition: () => void;
  setAdminPages: (adminPages: Extension[]) => void;
}

interface OwnProps {
  location: {};
}

type Props = StateProps & DispatchToProps & OwnProps;

class AdminContainer extends React.PureComponent<Props> {
  static contextTypes = {
    canAdmin: PropTypes.bool.isRequired
  };

  componentDidMount() {
    if (!this.context.canAdmin) {
      handleRequiredAuthorization();
    } else {
      this.fetchNavigationSettings();
      this.props.fetchCurrentEdition();
    }
  }

  fetchNavigationSettings = () =>
    getSettingsNavigation().then(r => this.props.setAdminPages(r.extensions), () => {});

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
          fetchPendingPlugins={this.props.fetchPendingPlugins}
          location={this.props.location}
          organizationsEnabled={organizationsEnabled}
          pendingPlugins={this.props.pendingPlugins}
        />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({
  appState: getAppState(state),
  pendingPlugins: getMarketplacePendingPlugins(state)
});

const mapDispatchToProps: DispatchToProps = {
  fetchCurrentEdition,
  fetchPendingPlugins,
  setAdminPages
};

export default connect<StateProps, DispatchToProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  AdminContainer
);
