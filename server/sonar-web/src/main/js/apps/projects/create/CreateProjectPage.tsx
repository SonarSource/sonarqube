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
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import { InjectedRouter } from 'react-router';
import { Location } from 'history';
import Helmet from 'react-helmet';
import AutoProjectCreate from './AutoProjectCreate';
import ManualProjectCreate from './ManualProjectCreate';
import { serializeQuery, Query, parseQuery } from './utils';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tabs from '../../../components/controls/Tabs';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import { addGlobalErrorMessage } from '../../../store/globalMessages';
import { skipOnboarding as skipOnboardingAction } from '../../../store/users';
import { CurrentUser, IdentityProvider, LoggedInUser } from '../../../app/types';
import { skipOnboarding, getIdentityProviders } from '../../../api/users';
import { hasAdvancedALMIntegration } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import '../../../app/styles/sonarcloud.css';

interface OwnProps {
  location: Location;
  router: Pick<InjectedRouter, 'push' | 'replace'>;
}

interface StateProps {
  currentUser: CurrentUser;
}

interface DispatchProps {
  addGlobalErrorMessage: (message: string) => void;
  skipOnboardingAction: () => void;
}

type Props = StateProps & DispatchProps & OwnProps;

interface State {
  identityProvider?: IdentityProvider;
  loading: boolean;
}

export class CreateProjectPage extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    if (isLoggedIn(this.props.currentUser)) {
      this.mounted = true;
      const query = parseQuery(this.props.location.query);
      if (query.error) {
        this.props.addGlobalErrorMessage(query.error);
      }
      if (!hasAdvancedALMIntegration(this.props.currentUser)) {
        this.setState({ loading: false });
        this.updateQuery({ manual: true });
      } else {
        this.fetchIdentityProviders();
      }
      document.body.classList.add('white-page');
      if (document.documentElement) {
        document.documentElement.classList.add('white-page');
      }
    } else {
      handleRequiredAuthentication();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    if (document.documentElement) {
      document.documentElement.classList.remove('white-page');
    }
  }

  handleProjectCreate = (projectKeys: string[]) => {
    skipOnboarding().catch(() => {});
    this.props.skipOnboardingAction();
    if (projectKeys.length > 1) {
      this.props.router.push({ pathname: '/projects' });
    } else if (projectKeys.length === 1) {
      this.props.router.push(getProjectUrl(projectKeys[0]));
    }
  };

  fetchIdentityProviders = () => {
    getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({
            identityProvider: identityProviders.find(
              identityProvider =>
                identityProvider.key === (this.props.currentUser as LoggedInUser).externalProvider
            ),
            loading: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  onTabChange = (tab: 'auto' | 'manual') => {
    this.updateQuery({ manual: tab === 'manual' });
  };

  updateQuery = (changes: Partial<Query>) => {
    this.props.router.replace({
      pathname: this.props.location.pathname,
      query: serializeQuery({ ...parseQuery(this.props.location.query), ...changes })
    });
  };

  render() {
    const { currentUser } = this.props;

    if (!isLoggedIn(currentUser)) {
      return null;
    }

    const { identityProvider, loading } = this.state;
    const query = parseQuery(this.props.location.query);
    const header = translate('onboarding.create_project.header');
    const hasAutoProvisioning = hasAdvancedALMIntegration(currentUser) && identityProvider;
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
              {hasAutoProvisioning && (
                <Tabs
                  onChange={this.onTabChange}
                  selected={query.manual ? 'manual' : 'auto'}
                  tabs={[
                    {
                      key: 'auto',
                      node: (
                        <>
                          {translate('onboarding.create_project.select_repositories')}
                          <span
                            className={classNames('beta-badge spacer-left', {
                              'is-muted': query.manual
                            })}>
                            {translate('beta')}
                          </span>
                        </>
                      )
                    },
                    { key: 'manual', node: translate('onboarding.create_project.create_manually') }
                  ]}
                />
              )}

              {query.manual || !hasAutoProvisioning || !identityProvider ? (
                <ManualProjectCreate
                  currentUser={currentUser}
                  onProjectCreate={this.handleProjectCreate}
                  organization={query.organization}
                />
              ) : (
                <AutoProjectCreate
                  identityProvider={identityProvider}
                  onProjectCreate={this.handleProjectCreate}
                />
              )}
            </>
          )}
        </div>
      </>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  currentUser: getCurrentUser(state)
});

const mapDispatchToProps: DispatchProps = { addGlobalErrorMessage, skipOnboardingAction };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CreateProjectPage);
