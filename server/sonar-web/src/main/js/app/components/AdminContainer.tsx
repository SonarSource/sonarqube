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
import {
  getAppState,
  getGlobalSettingValue,
  getMarketplaceEditionStatus
} from '../../store/rootReducer';
import { getSettingsNavigation } from '../../api/nav';
import { EditionStatus, getEditionStatus } from '../../api/marketplace';
import { setAdminPages } from '../../store/appState/duck';
import { fetchEditions, setEditionStatus } from '../../store/marketplace/actions';
import { translate } from '../../helpers/l10n';
import { Extension } from '../types';

interface Props {
  appState: {
    adminPages: Extension[];
    organizationsEnabled: boolean;
    version: string;
  };
  editionsUrl: string;
  editionStatus?: EditionStatus;
  fetchEditions: (url: string, version: string) => void;
  location: {};
  setAdminPages: (adminPages: Extension[]) => void;
  setEditionStatus: (editionStatus: EditionStatus) => void;
}

class AdminContainer extends React.PureComponent<Props> {
  static contextTypes = {
    canAdmin: PropTypes.bool.isRequired
  };

  componentDidMount() {
    if (!this.context.canAdmin) {
      // workaround cyclic dependencies
      import('../utils/handleRequiredAuthorization').then(handleRequredAuthorization =>
        handleRequredAuthorization.default()
      );
    } else {
      this.fetchNavigationSettings();
      this.props.fetchEditions(this.props.editionsUrl, this.props.appState.version);
      this.fetchEditionStatus();
    }
  }

  fetchNavigationSettings = () =>
    getSettingsNavigation().then(r => this.props.setAdminPages(r.extensions), () => {});

  fetchEditionStatus = () =>
    getEditionStatus().then(editionStatus => this.props.setEditionStatus(editionStatus), () => {});

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
          customOrganizations={organizationsEnabled}
          editionStatus={this.props.editionStatus}
          extensions={adminPages}
          location={this.props.location}
        />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state),
  editionStatus: getMarketplaceEditionStatus(state),
  editionsUrl: (getGlobalSettingValue(state, 'sonar.editions.jsonUrl') || {}).value
});

const mapDispatchToProps = { setAdminPages, setEditionStatus, fetchEditions };

export default connect(mapStateToProps, mapDispatchToProps)(AdminContainer as any);
