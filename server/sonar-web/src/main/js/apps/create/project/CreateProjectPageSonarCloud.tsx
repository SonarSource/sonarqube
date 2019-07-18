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
import { WithRouterProps } from 'react-router';
import Tabs from 'sonar-ui-common/components/controls/Tabs';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { getAlmAppInfo } from '../../../api/alm-integration';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import { hasAdvancedALMIntegration } from '../../../helpers/almIntegrations';
import { getOrganizationUrl, getProjectUrl } from '../../../helpers/urls';
import { skipOnboarding } from '../../../store/users';
import AutoProjectCreate from './AutoProjectCreate';
import ManualProjectCreate from './ManualProjectCreate';
import './style.css';

interface Props {
  currentUser: T.LoggedInUser;
  fetchMyOrganizations: () => Promise<void>;
  skipOnboarding: () => void;
  userOrganizations: T.Organization[];
}

interface State {
  almApplication?: T.AlmApplication;
  loading: boolean;
}

type TabKeys = 'auto' | 'manual';

interface LocationState {
  organization?: string;
  tab?: TabKeys;
}

export class CreateProjectPageSonarCloud extends React.PureComponent<
  Props & WithRouterProps,
  State
> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    if (hasAdvancedALMIntegration(this.props.currentUser)) {
      this.fetchAlmApplication();
    } else {
      this.setState({ loading: false });
    }
    addWhitePageClass();
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
  }

  handleProjectCreate = (projectKeys: string[], organization?: string) => {
    this.props.skipOnboarding();
    if (projectKeys.length > 1) {
      this.props.router.push({
        pathname: (organization ? getOrganizationUrl(organization) : '') + '/projects'
      });
    } else if (projectKeys.length === 1) {
      this.props.router.push(getProjectUrl(projectKeys[0]));
    }
  };

  fetchAlmApplication = () => {
    return getAlmAppInfo().then(
      ({ application }) => {
        if (this.mounted) {
          this.setState({ almApplication: application, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  onTabChange = (tab: TabKeys) => {
    this.updateUrl({ tab });
  };

  updateUrl = (state: Partial<LocationState> = {}) => {
    this.props.router.replace({
      pathname: this.props.location.pathname,
      query: this.props.location.query,
      state: { ...(this.props.location.state || {}), ...state }
    });
  };

  render() {
    const { currentUser, location, userOrganizations } = this.props;
    const { almApplication, loading } = this.state;
    const state: LocationState = location.state || {};
    const header = translate('onboarding.create_project.header');
    const showManualTab = state.tab === 'manual';

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="page page-limited huge-spacer-top huge-spacer-bottom">
          <header className="page-header huge-spacer-bottom">
            <h1 className="page-title huge">
              <strong>{header}</strong>
            </h1>
          </header>
          {loading ? (
            <DeferredSpinner />
          ) : (
            <>
              {almApplication && (
                <Tabs<TabKeys>
                  onChange={this.onTabChange}
                  selected={state.tab || 'auto'}
                  tabs={[
                    {
                      key: 'auto',
                      node: translate('onboarding.create_project.select_repositories')
                    },
                    { key: 'manual', node: translate('onboarding.create_project.setup_manually') }
                  ]}
                />
              )}

              {showManualTab || !almApplication ? (
                <ManualProjectCreate
                  currentUser={currentUser}
                  fetchMyOrganizations={this.props.fetchMyOrganizations}
                  onProjectCreate={this.handleProjectCreate}
                  organization={state.organization}
                  userOrganizations={userOrganizations.filter(
                    ({ actions = {} }) => actions.provision
                  )}
                />
              ) : (
                <AutoProjectCreate
                  almApplication={almApplication}
                  boundOrganizations={userOrganizations.filter(
                    ({ alm, actions = {} }) => alm && actions.provision
                  )}
                  onOrganizationUpgrade={this.props.fetchMyOrganizations}
                  onProjectCreate={this.handleProjectCreate}
                  organization={state.organization}
                />
              )}
            </>
          )}
        </div>
      </>
    );
  }
}

const mapDispatchToProps = { skipOnboarding };

export default whenLoggedIn(
  withUserOrganizations(
    connect(
      null,
      mapDispatchToProps
    )(CreateProjectPageSonarCloud)
  )
);
