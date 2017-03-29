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
import { connect } from 'react-redux';
import SettingsNav from './nav/settings/SettingsNav';
import { getCurrentUser, getAppState } from '../../store/rootReducer';
import { isUserAdmin } from '../../helpers/users';
import { onFail } from '../../store/rootActions';
import { getSettingsNavigation } from '../../api/nav';
import { setAdminPages } from '../../store/appState/duck';

class AdminContainer extends React.Component {
  componentDidMount() {
    if (!isUserAdmin(this.props.currentUser)) {
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
    if (!isUserAdmin(this.props.currentUser) || !this.props.adminPages) {
      return null;
    }

    return (
      <div>
        <SettingsNav location={this.props.location} extensions={this.props.adminPages} />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = state => ({
  adminPages: getAppState(state).adminPages,
  currentUser: getCurrentUser(state)
});

const mapDispatchToProps = { setAdminPages };

export default connect(mapStateToProps, mapDispatchToProps)(AdminContainer);
