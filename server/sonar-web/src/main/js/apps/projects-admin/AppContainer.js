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
import Main from './main';
import { getCurrentUser, getAppState } from '../../store/rootReducer';
import { getRootQualifiers } from '../../store/appState/duck';

class AppContainer extends React.Component {
  render () {
    const hasProvisionPermission = this.props.organization ?
        this.props.organization.canProvisionProjects :
        this.props.user.permissions.global.indexOf('provisioning') !== -1;

    const topLevelQualifiers = this.props.organization && !this.props.organization.isDefault ?
        ['TRK'] :
        this.props.rootQualifiers;

    return (
        <Main
            hasProvisionPermission={hasProvisionPermission}
            topLevelQualifiers={topLevelQualifiers}
            organization={this.props.organization}/>
    );
  }
}

const mapStateToProps = state => ({
  rootQualifiers: getRootQualifiers(getAppState(state)),
  user: getCurrentUser(state)
});

export default connect(
    mapStateToProps
)(AppContainer);
