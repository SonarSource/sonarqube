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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import OrganizationsList from './OrganizationsList';
import { translate } from '../../../helpers/l10n';
import { fetchIfAnyoneCanCreateOrganizations, fetchMyOrganizations } from './actions';
import { getMyOrganizations, getSettingValue, getCurrentUser } from '../../../store/rootReducer';
import type { Organization } from '../../../store/organizations/duck';
import { isUserAdmin } from '../../../helpers/users';

class UserOrganizations extends React.Component {
  mounted: boolean;

  props: {
    anyoneCanCreate?: { value: string },
    currentUser: Object,
    children: Object,
    organizations: Array<Organization>,
    fetchIfAnyoneCanCreateOrganizations: () => Promise<*>,
    fetchMyOrganizations: () => Promise<*>
  };

  state: { loading: boolean } = {
    loading: true
  };

  componentDidMount () {
    this.mounted = true;
    Promise.all([this.props.fetchMyOrganizations(), this.props.fetchIfAnyoneCanCreateOrganizations()]).then(() => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    });
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  render () {
    const title = translate('my_account.organizations') + ' - ' + translate('my_account.page');

    const anyoneCanCreate = this.props.anyoneCanCreate != null && this.props.anyoneCanCreate.value === 'true';

    const canCreateOrganizations = !this.state.loading && (anyoneCanCreate || isUserAdmin(this.props.currentUser));

    return (
        <div className="account-body account-container">
          <Helmet title={title} titleTemplate="%s - SonarQube"/>

          <header className="page-header">
            <h2 className="page-title">{translate('my_account.organizations')}</h2>
            {canCreateOrganizations && (
                <div className="page-actions">
                  <Link to="/account/organizations/create" className="button">{translate('create')}</Link>
                </div>
            )}
            {this.props.organizations.length > 0 ? (
                <div className="page-description">
                  {translate('my_account.organizations.description')}
                </div>
            ) : (
                <div className="page-description">
                  {translate('my_account.organizations.no_results')}
                </div>
            )}
          </header>

          {this.state.loading ? (
              <i className="spinner"/>
          ) : (
              <OrganizationsList organizations={this.props.organizations}/>
          )}

          {this.props.children}
        </div>
    );
  }
}

const mapStateToProps = state => ({
  anyoneCanCreate: getSettingValue(state, 'sonar.organizations.anyoneCanCreate'),
  currentUser: getCurrentUser(state),
  organizations: getMyOrganizations(state)
});

const mapDispatchToProps = {
  fetchMyOrganizations,
  fetchIfAnyoneCanCreateOrganizations
};

export default connect(mapStateToProps, mapDispatchToProps)(UserOrganizations);
