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
import { differenceInMinutes } from 'date-fns';
import { times } from 'lodash';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Helmet } from 'react-helmet';
import { FormattedMessage } from 'react-intl';
import { Link, withRouter, WithRouterProps } from 'react-router';
import {
  formatPrice,
  ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP,
  parseQuery,
  serializeQuery,
  Query,
  ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP
} from './utils';
import AlmApplicationInstalling from './AlmApplicationInstalling';
import AutoOrganizationCreate from './AutoOrganizationCreate';
import AutoPersonalOrganizationBind from './AutoPersonalOrganizationBind';
import ManualOrganizationCreate from './ManualOrganizationCreate';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tabs from '../../../components/controls/Tabs';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import {
  bindAlmOrganization,
  getAlmAppInfo,
  getAlmOrganization,
  GetAlmOrganizationResponse,
  listUnboundApplications
} from '../../../api/alm-integration';
import { getSubscriptionPlans } from '../../../api/billing';
import {
  AlmApplication,
  AlmOrganization,
  AlmUnboundApplication,
  LoggedInUser,
  Organization,
  OrganizationBase,
  SubscriptionPlan
} from '../../../app/types';
import { hasAdvancedALMIntegration, isPersonal } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { get, remove } from '../../../helpers/storage';
import { slugify } from '../../../helpers/strings';
import { getOrganizationUrl } from '../../../helpers/urls';
import { skipOnboarding } from '../../../store/users';
import * as api from '../../../api/organizations';
import * as actions from '../../../store/organizations';
import '../../../app/styles/sonarcloud.css';
import '../../tutorials/styles.css'; // TODO remove me

interface Props {
  createOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
  currentUser: LoggedInUser;
  deleteOrganization: (key: string) => Promise<void>;
  updateOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
  userOrganizations: Organization[];
  skipOnboarding: () => void;
}

interface State {
  almApplication?: AlmApplication;
  almOrganization?: AlmOrganization;
  almOrgLoading: boolean;
  almUnboundApplications: AlmUnboundApplication[];
  boundOrganization?: OrganizationBase;
  loading: boolean;
  organization?: Organization;
  subscriptionPlans?: SubscriptionPlan[];
}

type StateWithAutoImport = State & Required<Pick<State, 'almApplication'>>;

type TabKeys = 'auto' | 'manual';

interface LocationState {
  paid?: boolean;
  tab?: TabKeys;
}

export class CreateOrganization extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { almOrgLoading: false, almUnboundApplications: [], loading: true };

  componentDidMount() {
    this.mounted = true;
    document.body.classList.add('white-page');
    if (document.documentElement) {
      document.documentElement.classList.add('white-page');
    }
    const initRequests = [this.fetchSubscriptionPlans()];
    if (hasAdvancedALMIntegration(this.props.currentUser)) {
      initRequests.push(this.fetchAlmApplication());

      const query = parseQuery(this.props.location.query);
      if (query.almInstallId) {
        this.fetchAlmOrganization(query.almInstallId);
      } else {
        initRequests.push(this.fetchAlmUnboundApplications());
      }
    }
    Promise.all(initRequests).then(this.stopLoading, this.stopLoading);
  }

  componentDidUpdate(prevProps: WithRouterProps) {
    const prevQuery = parseQuery(prevProps.location.query);
    const query = parseQuery(this.props.location.query);
    if (this.state.almApplication && prevQuery.almInstallId !== query.almInstallId) {
      if (query.almInstallId) {
        this.fetchAlmOrganization(query.almInstallId);
      } else {
        this.setState({ almOrganization: undefined, boundOrganization: undefined, loading: true });
        this.fetchAlmUnboundApplications().then(this.stopLoading, this.stopLoading);
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    if (document.documentElement) {
      document.documentElement.classList.remove('white-page');
    }
  }

  fetchAlmApplication = () => {
    return getAlmAppInfo().then(({ application }) => {
      if (this.mounted) {
        this.setState({ almApplication: application });
      }
    });
  };

  fetchAlmUnboundApplications = () => {
    return listUnboundApplications().then(almUnboundApplications => {
      if (this.mounted) {
        this.setState({ almUnboundApplications });
      }
    });
  };

  hasAutoImport(state: State, paid?: boolean): state is StateWithAutoImport {
    return Boolean(state.almApplication && !paid);
  }

  setValidOrgKey = (almOrganization: AlmOrganization) => {
    const key = slugify(almOrganization.key);
    const keys = [key, ...times(9, i => `${key}-${i + 1}`)];
    return api
      .getOrganizations({ organizations: keys.join(',') })
      .then(
        ({ organizations }) => {
          const availableKey = keys.find(key => !organizations.find(o => o.key === key));
          return availableKey || `${key}-${Math.ceil(Math.random() * 1000) + 10}`;
        },
        () => key
      )
      .then(key => {
        return { almOrganization: { ...almOrganization, key } };
      });
  };

  fetchAlmOrganization = (installationId: string) => {
    this.setState({ almOrgLoading: true });
    return getAlmOrganization({ installationId })
      .then(({ almOrganization, boundOrganization }) => {
        if (boundOrganization) {
          return Promise.resolve({ almOrganization, boundOrganization });
        }
        return this.setValidOrgKey(almOrganization);
      })
      .then(
        ({ almOrganization, boundOrganization }: GetAlmOrganizationResponse) => {
          if (this.mounted) {
            if (
              boundOrganization &&
              boundOrganization.key &&
              !this.isStoredTimestampValid(ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP)
            ) {
              this.props.router.push({
                pathname: getOrganizationUrl(boundOrganization.key)
              });
            } else {
              this.setState({ almOrganization, almOrgLoading: false, boundOrganization });
            }
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ almOrgLoading: false });
          }
        }
      );
  };

  fetchSubscriptionPlans = () => {
    return getSubscriptionPlans().then(subscriptionPlans => {
      if (this.mounted) {
        this.setState({ subscriptionPlans });
      }
    });
  };

  handleOrgCreated = (organization: string, justCreated = true) => {
    this.props.skipOnboarding();
    if (this.isStoredTimestampValid(ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP)) {
      this.props.router.push({
        pathname: '/projects/create',
        state: { organization, tab: this.state.almOrganization ? 'auto' : 'manual' }
      });
    } else {
      this.props.router.push({
        pathname: getOrganizationUrl(organization),
        state: { justCreated }
      });
    }
  };

  isStoredTimestampValid = (timestampKey: string) => {
    const storedTimestamp = get(timestampKey);
    remove(timestampKey);
    return storedTimestamp && differenceInMinutes(Date.now(), Number(storedTimestamp)) < 10;
  };

  onTabChange = (tab: TabKeys) => {
    this.updateUrlState({ tab });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  updateUrlQuery = (query: Partial<Query> = {}) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: serializeQuery({ ...parseQuery(this.props.location.query), ...query }),
      state: this.props.location.state
    });
  };

  updateUrlState = (state: Partial<LocationState> = {}) => {
    this.props.router.replace({
      pathname: this.props.location.pathname,
      query: this.props.location.query,
      state: { ...(this.props.location.state || {}), ...state }
    });
  };

  renderContent = (almInstallId?: string, importPersonalOrg?: Organization) => {
    const { currentUser, location } = this.props;
    const { state } = this;
    const { almOrganization } = state;
    const { paid, tab = 'auto' } = (location.state || {}) as LocationState;

    if (importPersonalOrg && almOrganization && state.almApplication) {
      return (
        <AutoPersonalOrganizationBind
          almApplication={state.almApplication}
          almInstallId={almInstallId}
          almOrganization={almOrganization}
          importPersonalOrg={importPersonalOrg}
          onOrgCreated={this.handleOrgCreated}
          updateOrganization={this.props.updateOrganization}
          updateUrlQuery={this.updateUrlQuery}
        />
      );
    }

    return (
      <>
        {this.hasAutoImport(state, paid) && (
          <Tabs<TabKeys>
            onChange={this.onTabChange}
            selected={tab || 'auto'}
            tabs={[
              {
                key: 'auto',
                node: translate('onboarding.import_organization', state.almApplication.key)
              },
              {
                key: 'manual',
                node: translate('onboarding.create_organization.create_manually')
              }
            ]}
          />
        )}

        <ManualOrganizationCreate
          className={classNames({ hidden: tab !== 'manual' && this.hasAutoImport(state, paid) })}
          createOrganization={this.props.createOrganization}
          deleteOrganization={this.props.deleteOrganization}
          onOrgCreated={this.handleOrgCreated}
          onlyPaid={paid}
          subscriptionPlans={this.state.subscriptionPlans}
        />

        {this.hasAutoImport(state, paid) && (
          <AutoOrganizationCreate
            almApplication={state.almApplication}
            almInstallId={almInstallId}
            almOrganization={almOrganization}
            almUnboundApplications={this.state.almUnboundApplications}
            boundOrganization={this.state.boundOrganization}
            className={classNames({ hidden: tab !== 'auto' })}
            createOrganization={this.props.createOrganization}
            onOrgCreated={this.handleOrgCreated}
            unboundOrganizations={this.props.userOrganizations.filter(
              ({ actions = {}, alm, key }) =>
                !alm && key !== currentUser.personalOrganization && actions.admin
            )}
            updateUrlQuery={this.updateUrlQuery}
          />
        )}
      </>
    );
  };

  render() {
    const { currentUser, location } = this.props;
    const query = parseQuery(location.query);

    if (this.state.almOrgLoading) {
      return <AlmApplicationInstalling almKey={query.almKey} />;
    }

    const { almOrganization, subscriptionPlans } = this.state;
    const importPersonalOrg = isPersonal(almOrganization)
      ? this.props.userOrganizations.find(o => o.key === currentUser.personalOrganization)
      : undefined;
    const header = importPersonalOrg
      ? translate('onboarding.import_organization.personal.page.header')
      : translate('onboarding.create_organization.page.header');
    const startedPrice = subscriptionPlans && subscriptionPlans[0] && subscriptionPlans[0].price;
    const formattedPrice = formatPrice(startedPrice);

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="sonarcloud page page-limited">
          <header className="page-header">
            <h1 className="page-title big-spacer-bottom">{header}</h1>
            {!importPersonalOrg &&
              startedPrice !== undefined && (
                <p className="page-description">
                  <FormattedMessage
                    defaultMessage={translate('onboarding.create_organization.page.description')}
                    id="onboarding.create_organization.page.description"
                    values={{
                      break: <br />,
                      price: formattedPrice,
                      more: (
                        <Link target="_blank" to="/documentation/sonarcloud-pricing/">
                          {translate('learn_more')}
                        </Link>
                      )
                    }}
                  />
                </p>
              )}
          </header>
          {this.state.loading ? (
            <DeferredSpinner />
          ) : (
            this.renderContent(query.almInstallId, importPersonalOrg)
          )}
        </div>
      </>
    );
  }
}

function createOrganization(organization: OrganizationBase & { installationId?: string }) {
  return (dispatch: Dispatch) => {
    return api.createOrganization(organization).then((organization: Organization) => {
      dispatch(actions.createOrganization(organization));
      return organization;
    });
  };
}

function updateOrganization(
  organization: OrganizationBase & { key: string; installationId?: string }
) {
  return (dispatch: Dispatch) => {
    const { key, installationId, ...changes } = organization;
    const promises = [api.updateOrganization(key, changes)];
    if (installationId) {
      promises.push(bindAlmOrganization({ organization: key, installationId }));
    }
    return Promise.all(promises).then(() => {
      dispatch(actions.updateOrganization(key, changes));
      return organization;
    });
  };
}

function deleteOrganization(key: string) {
  return (dispatch: Dispatch) => {
    return api.deleteOrganization(key).then(() => {
      dispatch(actions.deleteOrganization(key));
    });
  };
}

const mapDispatchToProps = {
  createOrganization: createOrganization as any,
  deleteOrganization: deleteOrganization as any,
  updateOrganization: updateOrganization as any,
  skipOnboarding: skipOnboarding as any
};

export default whenLoggedIn(
  withUserOrganizations(
    withRouter(
      connect(
        null,
        mapDispatchToProps
      )(CreateOrganization)
    )
  )
);
