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
import { times } from 'lodash';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Helmet } from 'react-helmet';
import { FormattedMessage } from 'react-intl';
import { Link, withRouter, WithRouterProps } from 'react-router';
import { formatPrice, parseQuery } from './utils';
import AlmApplicationInstalling from './AlmApplicationInstalling';
import AutoOrganizationCreate from './AutoOrganizationCreate';
import AutoPersonalOrganizationBind from './AutoPersonalOrganizationBind';
import ManualOrganizationCreate from './ManualOrganizationCreate';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tabs from '../../../components/controls/Tabs';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import {
  getAlmAppInfo,
  getAlmOrganization,
  bindAlmOrganization
} from '../../../api/alm-integration';
import { getSubscriptionPlans } from '../../../api/billing';
import {
  LoggedInUser,
  Organization,
  SubscriptionPlan,
  AlmApplication,
  AlmOrganization,
  OrganizationBase
} from '../../../app/types';
import { hasAdvancedALMIntegration, isPersonal } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { slugify } from '../../../helpers/strings';
import { getOrganizationUrl } from '../../../helpers/urls';
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
}

interface State {
  almApplication?: AlmApplication;
  almOrganization?: AlmOrganization;
  almOrgLoading: boolean;
  loading: boolean;
  organization?: Organization;
  subscriptionPlans?: SubscriptionPlan[];
}

type TabKeys = 'auto' | 'manual';

interface LocationState {
  paid?: boolean;
  tab?: TabKeys;
}

export class CreateOrganization extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { almOrgLoading: false, loading: true };

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
      }
    }
    Promise.all(initRequests).then(this.stopLoading, this.stopLoading);
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
  }

  fetchAlmApplication = () => {
    return getAlmAppInfo().then(({ application }) => {
      if (this.mounted) {
        this.setState({ almApplication: application });
      }
    });
  };

  fetchValidOrgKey = (almOrganization: AlmOrganization) => {
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
        return { ...almOrganization, key };
      });
  };

  fetchAlmOrganization = (installationId: string) => {
    this.setState({ almOrgLoading: true });
    return getAlmOrganization({ installationId })
      .then(this.fetchValidOrgKey)
      .then(almOrganization => {
        if (this.mounted) {
          this.setState({ almOrganization, almOrgLoading: false });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ almOrgLoading: false });
        }
      });
  };

  fetchSubscriptionPlans = () => {
    return getSubscriptionPlans().then(subscriptionPlans => {
      if (this.mounted) {
        this.setState({ subscriptionPlans });
      }
    });
  };

  handleOrgCreated = (organization: string, justCreated = true) => {
    this.props.router.push({
      pathname: getOrganizationUrl(organization),
      state: { justCreated }
    });
  };

  onTabChange = (tab: TabKeys) => {
    this.updateUrl({ tab });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  updateUrl = (state: Partial<LocationState> = {}) => {
    this.props.router.replace({
      pathname: this.props.location.pathname,
      query: this.props.location.query,
      state: { ...(this.props.location.state || {}), ...state }
    });
  };

  renderContent = (almInstallId?: string, importPersonalOrg?: Organization) => {
    const { currentUser, location } = this.props;
    const { almApplication, almOrganization } = this.state;
    const state = (location.state || {}) as LocationState;

    if (importPersonalOrg && almOrganization && almApplication) {
      return (
        <AutoPersonalOrganizationBind
          almApplication={almApplication}
          almInstallId={almInstallId}
          almOrganization={almOrganization}
          importPersonalOrg={importPersonalOrg}
          onOrgCreated={this.handleOrgCreated}
          updateOrganization={this.props.updateOrganization}
        />
      );
    }

    const showManualTab = state.tab === 'manual' && !almOrganization;
    return (
      <>
        {almApplication && (
          <Tabs<TabKeys>
            onChange={this.onTabChange}
            selected={showManualTab ? 'manual' : 'auto'}
            tabs={[
              {
                key: 'auto',
                node: translate('onboarding.import_organization', almApplication.key)
              },
              {
                disabled: Boolean(almOrganization),
                key: 'manual',
                node: translate('onboarding.create_organization.create_manually')
              }
            ]}
          />
        )}

        {showManualTab || !almApplication ? (
          <ManualOrganizationCreate
            createOrganization={this.props.createOrganization}
            deleteOrganization={this.props.deleteOrganization}
            onOrgCreated={this.handleOrgCreated}
            onlyPaid={state.paid}
            subscriptionPlans={this.state.subscriptionPlans}
          />
        ) : (
          <AutoOrganizationCreate
            almApplication={almApplication}
            almInstallId={almInstallId}
            almOrganization={almOrganization}
            createOrganization={this.props.createOrganization}
            onOrgCreated={this.handleOrgCreated}
            unboundOrganizations={this.props.userOrganizations.filter(
              o => !o.alm && o.key !== currentUser.personalOrganization
            )}
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
    const description = importPersonalOrg
      ? translate('onboarding.import_organization.personal.page.description')
      : translate('onboarding.create_organization.page.description');
    const startedPrice = subscriptionPlans && subscriptionPlans[0] && subscriptionPlans[0].price;
    const formattedPrice = formatPrice(startedPrice);

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="sonarcloud page page-limited">
          <header className="page-header">
            <h1 className="page-title big-spacer-bottom">{header}</h1>
            {startedPrice !== undefined && (
              <p className="page-description">
                <FormattedMessage
                  defaultMessage={description}
                  id={description}
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
  updateOrganization: updateOrganization as any
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
