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
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import SettingsNav from './nav/settings/SettingsNav';
import { getAppState } from '../../store/rootReducer';
import { getSettingsNavigation } from '../../api/nav';
import { setAdminPages } from '../../store/appState/duck';
import { translate } from '../../helpers/l10n';
import { Extension } from '../types';

interface Props {
  appState: {
    adminPages: Extension[];
    organizationsEnabled: boolean;
  };
  location: {};
  setAdminPages: (adminPages: Extension[]) => void;
}

class AdminContainer extends React.PureComponent<Props> {
  static contextTypes = {
    canAdmin: PropTypes.bool.isRequired
  };

  componentDidMount() {
    if (!this.context.canAdmin) {
      // workaround cyclic dependencies
      const handleRequiredAuthorization = require('../utils/handleRequiredAuthorization').default;
      handleRequiredAuthorization();
    } else {
      this.loadData();
    }
  }

  loadData() {
    getSettingsNavigation().then(
      r => {
        this.props.setAdminPages(r.extensions);
      },
      () => {}
    );
  }

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
          extensions={adminPages}
          location={this.props.location}
        />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state)
});

const mapDispatchToProps = { setAdminPages };

export default connect(mapStateToProps, mapDispatchToProps)(AdminContainer as any);
