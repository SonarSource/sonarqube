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
import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import SettingsNav from './nav/settings/SettingsNav';
import { getAppState } from '../../store/rootReducer';
import { onFail } from '../../store/rootActions';
import { getSettingsNavigation } from '../../api/nav';
import { setAdminPages } from '../../store/appState/duck';
import { translate } from '../../helpers/l10n';

class AdminContainer extends React.PureComponent {
  componentDidMount() {
    if (!this.props.appState.canAdmin) {
      // workaround cyclic dependencies
      const handleRequiredAuthorization = require('../utils/handleRequiredAuthorization').default;
      handleRequiredAuthorization();
    }
    this.loadData();
  }

  loadData() {
    getSettingsNavigation().then(
      r => this.props.setAdminPages(r.extensions),
      onFail(this.props.dispatch)
    );
  }

  render() {
    const { adminPages } = this.props.appState;

    // Check that the adminPages are loaded
    if (!adminPages) {
      return null;
    }

    return (
      <div>
        <Helmet title={translate('layout.settings')} />
        <SettingsNav location={this.props.location} extensions={adminPages} />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = state => ({
  appState: getAppState(state)
});

const mapDispatchToProps = { setAdminPages };

export default connect(mapStateToProps, mapDispatchToProps)(AdminContainer);
