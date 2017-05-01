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
import { onFail } from '../../store/rootActions';
import { getCurrentUser, getAppState } from '../../store/rootReducer';
import { getRootQualifiers } from '../../store/appState/duck';
import { receiveOrganizations } from '../../store/organizations/duck';
import { changeProjectVisibility } from '../../api/organizations';

function AppContainer(props) {
  const hasProvisionPermission = props.organization
    ? props.organization.canProvisionProjects
    : props.user.permissions.global.indexOf('provisioning') !== -1;

  const topLevelQualifiers = props.organization && !props.organization.isDefault
    ? ['TRK']
    : props.rootQualifiers;

  return (
    <Main
      hasProvisionPermission={hasProvisionPermission}
      topLevelQualifiers={topLevelQualifiers}
      onVisibilityChange={props.onVisibilityChange}
      organization={props.organization}
    />
  );
}

const mapStateToProps = state => ({
  rootQualifiers: getRootQualifiers(getAppState(state)),
  user: getCurrentUser(state)
});

const onVisibilityChange = (organization, visibility) => dispatch => {
  const currentVisibility = organization.projectVisibility;
  dispatch(receiveOrganizations([{ ...organization, projectVisibility: visibility }]));
  changeProjectVisibility(organization.key, visibility).catch(error => {
    onFail(dispatch)(error);
    dispatch(receiveOrganizations([{ ...organization, projectVisibility: currentVisibility }]));
  });
};

const mapDispatchToProps = (dispatch, ownProps) => ({
  onVisibilityChange: visibility => dispatch(onVisibilityChange(ownProps.organization, visibility))
});

export default connect(mapStateToProps, mapDispatchToProps)(AppContainer);
