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
import { connect } from 'react-redux';
import App from './App';
import { Organization } from '../../app/types';
import { onFail } from '../../store/rootActions';
import { getAppState, getOrganizationByKey, getCurrentUser } from '../../store/rootReducer';
import { receiveOrganizations } from '../../store/organizations/duck';
import { changeProjectVisibility } from '../../api/organizations';
import { fetchOrganization } from '../../apps/organizations/actions';

interface Props {
  appState: {
    defaultOrganization: string;
    qualifiers: string[];
  };
  currentUser: { login: string };
  fetchOrganization: (organization: string) => void;
  onVisibilityChange: (organization: Organization, visibility: string) => void;
  onRequestFail: (error: any) => void;
  organization?: Organization;
}

class AppContainer extends React.PureComponent<Props> {
  componentDidMount() {
    // if there is no organization, that means we are in the global scope
    // let's fetch defails for the default organization in this case
    if (!this.props.organization || !this.props.organization.projectVisibility) {
      this.props.fetchOrganization(this.props.appState.defaultOrganization);
    }
  }

  handleVisibilityChange = (visibility: string) => {
    if (this.props.organization) {
      this.props.onVisibilityChange(this.props.organization, visibility);
    }
  };

  render() {
    const { organization } = this.props;

    if (!organization) {
      return null;
    }

    const topLevelQualifiers = organization.isDefault ? this.props.appState.qualifiers : ['TRK'];

    return (
      <App
        currentUser={this.props.currentUser}
        hasProvisionPermission={organization.canProvisionProjects}
        onVisibilityChange={this.handleVisibilityChange}
        organization={organization}
        topLevelQualifiers={topLevelQualifiers}
      />
    );
  }
}

const mapStateToProps = (state: any, ownProps: Props) => ({
  appState: getAppState(state),
  currentUser: getCurrentUser(state),
  organization:
    ownProps.organization || getOrganizationByKey(state, getAppState(state).defaultOrganization)
});

const onVisibilityChange = (organization: Organization, visibility: string) => (
  dispatch: Function
) => {
  const currentVisibility = organization.projectVisibility;
  dispatch(receiveOrganizations([{ ...organization, projectVisibility: visibility }]));
  changeProjectVisibility(organization.key, visibility).catch(error => {
    onFail(dispatch)(error);
    dispatch(receiveOrganizations([{ ...organization, projectVisibility: currentVisibility }]));
  });
};

const mapDispatchToProps = (dispatch: Function) => ({
  fetchOrganization: (key: string) => dispatch(fetchOrganization(key)),
  onVisibilityChange: (organization: Organization, visibility: string) =>
    dispatch(onVisibilityChange(organization, visibility))
});

export default connect<any, any, any>(mapStateToProps, mapDispatchToProps)(AppContainer);
