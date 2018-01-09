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
import { RouterState } from 'react-router';
import { getOrganizationByKey } from '../../../store/rootReducer';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { Organization } from '../../../app/types';

interface StateToProps {
  organization?: Organization;
}

interface OwnProps extends RouterState {
  children: JSX.Element;
}

interface Props extends StateToProps, Pick<OwnProps, 'children' | 'location'> {}

export class OrganizationAdmin extends React.PureComponent<Props> {
  componentDidMount() {
    this.checkPermissions();
  }

  componentDidUpdate() {
    this.checkPermissions();
  }

  isOrganizationAdmin = () => this.props.organization && this.props.organization.canAdmin;

  checkPermissions = () => {
    if (!this.isOrganizationAdmin()) {
      handleRequiredAuthorization();
    }
  };

  render() {
    if (!this.isOrganizationAdmin()) {
      return null;
    }
    return React.cloneElement(this.props.children, {
      location: this.props.location,
      organization: this.props.organization
    });
  }
}

const mapStateToProps = (state: any, ownProps: OwnProps) => ({
  organization: getOrganizationByKey(state, ownProps.params.organizationKey)
});

export default connect<StateToProps, {}, OwnProps>(mapStateToProps)(OrganizationAdmin);
