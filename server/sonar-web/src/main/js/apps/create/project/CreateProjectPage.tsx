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
import { WithRouterProps } from 'react-router';
import Helmet from 'react-helmet';
import AutoProjectCreate from './AutoProjectCreate';
import ManualProjectCreate from './ManualProjectCreate';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tabs from '../../../components/controls/Tabs';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import { skipOnboarding } from '../../../store/users';
import { LoggedInUser, AlmApplication, Organization } from '../../../app/types';
import { getAlmAppInfo } from '../../../api/alm-integration';
import { hasAdvancedALMIntegration } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl, getOrganizationUrl } from '../../../helpers/urls';
import '../../../app/styles/sonarcloud.css';

interface Props {
  currentUser: LoggedInUser;
  skipOnboarding: () => void;
  userOrganizations: Organization[];
}

interface State {
  almApplication?: AlmApplication;
  loading: boolean;
}

type TabKeys = 'auto' | 'manual';

interface LocationState {
  organization?: string;
  tab?: TabKeys;
}

export class CreateProjectPage extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    if (hasAdvancedALMIntegration(this.props.currentUser)) {
      this.fetchAlmApplication();
    } else {
      this.setState({ loading: false });
    }
    document.body.classList.add('white-page');
    if (document.documentElement) {
      document.documentElement.classList.add('white-page');
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    if (document.documentElement) {
      document.documentElement.classList.remove('white-page');
    }
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
        <div className="sonarcloud page page-limited">
          <header className="page-header">
            <h1 className="page-title">{header}</h1>
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
    )(CreateProjectPage)
  )
);
