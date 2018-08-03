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
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import { getCurrentUser } from '../../../store/rootReducer';
import { addGlobalErrorMessage } from '../../../store/globalMessages/duck';
import { skipOnboarding as skipOnboardingAction } from '../../../store/users/actions';
import { CurrentUser, IdentityProvider, isLoggedIn, LoggedInUser } from '../../../app/types';
import { skipOnboarding, getIdentityProviders } from '../../../api/users';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
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

interface Props extends OwnProps, StateProps, DispatchProps {
  currentUser: LoggedInUser;
}

interface State {
  identityProvider?: IdentityProvider;
  loading: boolean;
}

export class CreateProjectPage extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { loading: true };
    const query = parseQuery(props.location.query);
    if (query.error) {
      this.props.addGlobalErrorMessage(query.error);
    }
    if (!this.canAutoCreate(props)) {
      this.updateQuery({ manual: true });
    }
  }

  componentDidMount() {
    this.mounted = true;
    if (!isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }
    document.body.classList.add('white-page');
    document.documentElement.classList.add('white-page');
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    document.documentElement.classList.remove('white-page');
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

  canAutoCreate = ({ currentUser } = this.props) => {
    return ['bitbucket', 'github'].includes(currentUser.externalProvider || '');
  };

  fetchIdentityProviders = () => {
    getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({
            identityProvider: identityProviders.find(
              identityProvider => identityProvider.key === this.props.currentUser.externalProvider
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

  showAuto = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.updateQuery({ manual: false });
  };

  showManual = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.updateQuery({ manual: true });
  };

  updateQuery = (changes: Partial<Query>) => {
    this.props.router.replace({
      pathname: this.props.location.pathname,
      query: serializeQuery({ ...parseQuery(this.props.location.query), ...changes })
    });
  };

  render() {
    const { currentUser } = this.props;
    const { identityProvider, loading } = this.state;
    const displayManual = parseQuery(this.props.location.query).manual;
    const header = translate('onboarding.create_project.header');
    const hasAutoProvisioning = this.canAutoCreate() && identityProvider;
    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="sonarcloud page page-limited">
          <div className="page-header">
            <h1 className="page-title">{header}</h1>
          </div>
          {loading ? (
            <DeferredSpinner />
          ) : (
            <>
              {hasAutoProvisioning && (
                <ul className="flex-tabs">
                  <li>
                    <a
                      className={classNames('js-auto', { selected: !displayManual })}
                      href="#"
                      onClick={this.showAuto}>
                      {translate('onboarding.create_project.select_repositories')}
                      <span
                        className={classNames(
                          'rounded alert alert-small spacer-left display-inline-block',
                          {
                            'alert-info': !displayManual,
                            'alert-muted': displayManual
                          }
                        )}>
                        {translate('beta')}
                      </span>
                    </a>
                  </li>
                  <li>
                    <a
                      className={classNames('js-manual', { selected: displayManual })}
                      href="#"
                      onClick={this.showManual}>
                      {translate('onboarding.create_project.create_manually')}
                    </a>
                  </li>
                </ul>
              )}

              {displayManual || !hasAutoProvisioning ? (
                <ManualProjectCreate
                  currentUser={currentUser}
                  onProjectCreate={this.handleProjectCreate}
                />
              ) : (
                <AutoProjectCreate
                  identityProvider={identityProvider!}
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

const mapStateToProps = (state: any): StateProps => {
  return {
    currentUser: getCurrentUser(state)
  };
};

const mapDispatchToProps: DispatchProps = { addGlobalErrorMessage, skipOnboardingAction };

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  CreateProjectPage
);
