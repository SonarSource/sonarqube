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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  getAppState,
  getGlobalSettingValue,
  getMyOrganizations,
  Store
} from '../../../store/rootReducer';
import { fetchIfAnyoneCanCreateOrganizations } from './actions';
import OrganizationsList from './OrganizationsList';

interface StateProps {
  anyoneCanCreate: boolean;
  canAdmin?: boolean;
  organizations: T.Organization[];
}

interface DispatchProps {
  fetchIfAnyoneCanCreateOrganizations: () => Promise<void>;
}

interface Props extends StateProps, DispatchProps {}

interface State {
  loading: boolean;
}

class UserOrganizations extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchIfAnyoneCanCreateOrganizations().then(this.stopLoading, this.stopLoading);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { anyoneCanCreate } = this.props;
    const canCreateOrganizations = !this.state.loading && (anyoneCanCreate || this.props.canAdmin);

    return (
      <div className="account-body account-container">
        <Helmet title={translate('my_account.organizations')} />

        <div className="boxed-group">
          {canCreateOrganizations && (
            <div className="clearfix">
              <div className="boxed-group-actions">
                <Link className="button" to="/create-organization">
                  {translate('create')}
                </Link>
              </div>
            </div>
          )}
          <div className="boxed-group-inner">
            {this.state.loading ? (
              <i className="spinner" />
            ) : (
              <OrganizationsList organizations={this.props.organizations} />
            )}
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const anyoneCanCreate = getGlobalSettingValue(state, 'sonar.organizations.anyoneCanCreate');
  return {
    anyoneCanCreate: Boolean(anyoneCanCreate && anyoneCanCreate.value === 'true'),
    canAdmin: getAppState(state).canAdmin,
    organizations: getMyOrganizations(state)
  };
};

const mapDispatchToProps = {
  fetchIfAnyoneCanCreateOrganizations: fetchIfAnyoneCanCreateOrganizations as any
} as DispatchProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UserOrganizations);
