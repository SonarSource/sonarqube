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
import { connect } from 'react-redux';
import { RouterState } from 'react-router';
import { getCurrentUser, getOrganizationByKey, Store } from '../../../store/rootReducer';

interface StateToProps {
  organization?: T.Organization;
  currentUser: T.CurrentUser;
}

interface OwnProps extends RouterState {
  children: JSX.Element;
}

interface Props extends StateToProps, Pick<OwnProps, 'children' | 'location'> {}

class OrganizationContainer extends React.PureComponent<Props> {
  render() {
    return React.cloneElement(this.props.children, {
      location: this.props.location,
      currentUser: this.props.currentUser,
      organization: this.props.organization
    });
  }
}

const mapStateToProps = (state: Store, ownProps: OwnProps) => ({
  organization: getOrganizationByKey(state, ownProps.params.organizationKey),
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(OrganizationContainer);
