/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import OrganizationNavigation from '../navigation/OrganizationNavigation';
import { fetchOrganization } from '../actions';
import { getOrganizationByKey } from '../../../store/rootReducer';
import type { Organization } from '../../../store/organizations/duck';

type OwnProps = {
  params: { organizationKey: string }
};

class OrganizationPage extends React.Component {
  props: {
    organization: null | Organization,
    params: { organizationKey: string },
    fetchOrganization: (string) => void
  };

  componentDidMount () {
    this.props.fetchOrganization(this.props.params.organizationKey);
  }

  render () {
    const { organization } = this.props;

    if (!organization) {
      return null;
    }

    return (
        <div>
          <OrganizationNavigation organization={organization}/>
          {this.props.children}
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps: OwnProps) => ({
  organization: getOrganizationByKey(state, ownProps.params.organizationKey)
});

const mapDispatchToProps = { fetchOrganization };

export default connect(mapStateToProps, mapDispatchToProps)(OrganizationPage);
