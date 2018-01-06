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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import OrganizationNavigation from '../navigation/OrganizationNavigation';
import NotFound from '../../../app/components/NotFound';
import { fetchOrganization } from '../actions';
import { getOrganizationByKey } from '../../../store/rootReducer';
/*:: import type { Organization } from '../../../store/organizations/duck'; */

/*::
type OwnProps = {
  params: { organizationKey: string }
};
*/

/*::
type Props = {
  children?: React.Element<*>,
  location: Object,
  organization: null | Organization,
  params: { organizationKey: string },
  fetchOrganization: string => Promise<*>
};
*/

/*::
type State = {
  loading: boolean
};
*/

export class OrganizationPage extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.updateOrganization(this.props.params.organizationKey);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.params.organizationKey !== this.props.params.organizationKey) {
      this.updateOrganization(nextProps.params.organizationKey);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  updateOrganization = (organizationKey /*: string */) => {
    if (this.mounted) {
      this.setState({ loading: true });
    }
    this.props.fetchOrganization(organizationKey).then(() => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    });
  };

  render() {
    const { organization } = this.props;

    if (!organization || organization.canAdmin == null) {
      if (this.state.loading) {
        return null;
      } else {
        return <NotFound />;
      }
    }

    return (
      <div>
        <Helmet defaultTitle={organization.name} titleTemplate={'%s - ' + organization.name} />
        <OrganizationNavigation organization={organization} location={this.props.location} />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps /*: OwnProps */) => ({
  organization: getOrganizationByKey(state, ownProps.params.organizationKey)
});

const mapDispatchToProps = { fetchOrganization };

export default connect(mapStateToProps, mapDispatchToProps)(OrganizationPage);
