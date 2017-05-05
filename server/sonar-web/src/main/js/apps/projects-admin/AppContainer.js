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
import { getAppState, getOrganizationByKey } from '../../store/rootReducer';
import { receiveOrganizations } from '../../store/organizations/duck';
import { changeProjectVisibility } from '../../api/organizations';
import { fetchOrganization } from '../../apps/organizations/actions';

class AppContainer extends React.PureComponent {
  componentDidMount() {
    // if there is no organization, that means we are in the global scope
    // let's fetch defails for the default organization in this case
    if (!this.props.organization || !this.props.organization.projectVisibility) {
      this.props.fetchOrganization(this.props.appState.defaultOrganization);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleVisibilityChange = visibility => {
    this.props.onVisibilityChange(this.props.organization, visibility);
  };

  render() {
    const { organization } = this.props;

    if (!organization) {
      return null;
    }

    const topLevelQualifiers = organization.isDefault ? this.props.appState.qualifiers : ['TRK'];

    return (
      <Main
        hasProvisionPermission={organization.canProvisionProjects}
        topLevelQualifiers={topLevelQualifiers}
        onVisibilityChange={this.handleVisibilityChange}
        onRequestFail={this.props.onRequestFail}
        organization={organization}
      />
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  appState: getAppState(state),
  organization: ownProps.organization ||
    getOrganizationByKey(state, getAppState(state).defaultOrganization)
});

const onVisibilityChange = (organization, visibility) => dispatch => {
  const currentVisibility = organization.projectVisibility;
  dispatch(receiveOrganizations([{ ...organization, projectVisibility: visibility }]));
  changeProjectVisibility(organization.key, visibility).catch(error => {
    onFail(dispatch)(error);
    dispatch(receiveOrganizations([{ ...organization, projectVisibility: currentVisibility }]));
  });
};

const mapDispatchToProps = dispatch => ({
  fetchOrganization: key => dispatch(fetchOrganization(key)),
  onVisibilityChange: (organization, visibility) =>
    dispatch(onVisibilityChange(organization, visibility)),
  onRequestFail: error => onFail(dispatch)(error)
});

export default connect(mapStateToProps, mapDispatchToProps)(AppContainer);
