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
import * as classNames from 'classnames';
import * as differenceInMinutes from 'date-fns/difference_in_minutes';
import { times } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet';
import { connect } from 'react-redux';
import { withRouter, WithRouterProps } from 'react-router';
import Tabs from 'sonar-ui-common/components/controls/Tabs';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { get, remove } from 'sonar-ui-common/helpers/storage';
import { slugify } from 'sonar-ui-common/helpers/strings';
import {
  bindAlmOrganization,
  getAlmAppInfo,
  getAlmOrganization,
  GetAlmOrganizationResponse,
  listUnboundApplications
} from '../../../api/alm-integration';
import { getSubscriptionPlans } from '../../../api/billing';
import * as api from '../../../api/organizations';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import { hasAdvancedALMIntegration, sanitizeAlmId } from '../../../helpers/almIntegrations';
import { getOrganizationUrl } from '../../../helpers/urls';
import { skipOnboarding } from '../../../store/users';
import { deleteOrganization } from '../../organizations/actions';
import '../../tutorials/styles.css'; // TODO remove me
import { createOrganization } from './actions';
import AlmApplicationInstalling from './AlmApplicationInstalling';
import AutoOrganizationCreate from './AutoOrganizationCreate';
import ManualOrganizationCreate from './ManualOrganizationCreate';
import RemoteOrganizationChoose from './RemoteOrganizationChoose';
import {
  BIND_ORGANIZATION_KEY,
  BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP,
  ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP,
  ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP,
  parseQuery,
  Query,
  serializeQuery,
  Step
} from './utils';

interface Props {
  createOrganization: (
    organization: T.Organization & { installationId?: string }
  ) => Promise<string>;
  currentUser: T.LoggedInUser;
  deleteOrganization: (key: string) => Promise<void>;
  userOrganizations: T.Organization[];
  skipOnboarding: () => void;
}

interface State {
  almApplication?: T.AlmApplication;
  almOrganization?: T.AlmOrganization;
  almOrgLoading: boolean;
  almUnboundApplications: T.AlmUnboundApplication[];
  bindingExistingOrg: boolean;
  boundOrganization?: T.OrganizationBase;
  loading: boolean;
  organization?: T.Organization;
  step: Step;
  subscriptionPlans?: T.SubscriptionPlan[];
}

type StateWithAutoImport = State & Required<Pick<State, 'almApplication'>>;

type TabKeys = 'auto' | 'manual';

interface LocationState {
  tab?: TabKeys;
}

export class CreateOrganization extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = {
    almOrgLoading: false,
    almUnboundApplications: [],
    bindingExistingOrg: false,
    loading: true,
    step: Step.OrganizationDetails
  };

  componentDidMount() {
    this.mounted = true;
    addWhitePageClass();

    const query = parseQuery(this.props.location.query);

    //highjack the process for the organization settings
    if (
      hasAdvancedALMIntegration(this.props.currentUser) &&
      query.almInstallId &&
      this.isStoredTimestampValid(BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP)
    ) {
      this.bindAndRedirectToOrganizationSettings(query.almInstallId);
    } else {
      const initRequests = [this.fetchSubscriptionPlans()];
      if (hasAdvancedALMIntegration(this.props.currentUser)) {
        initRequests.push(this.fetchAlmApplication());

        if (query.almInstallId) {
          this.fetchAlmOrganization(query.almInstallId);
        } else {
          initRequests.push(this.fetchAlmUnboundApplications());
        }
      }
      Promise.all(initRequests).then(this.stopLoading, this.stopLoading);
    }
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
    removeWhitePageClass();
  }

  deleteOrganization = () => {
    if (this.state.organization) {
      this.props.deleteOrganization(this.state.organization.key);
    }
  };

  fetchAlmApplication = () => {
    return getAlmAppInfo().then(({ application }) => {
      if (this.mounted) {
        this.setState({ almApplication: application });
      }
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

  fetchAlmUnboundApplications = () => {
    return listUnboundApplications().then(almUnboundApplications => {
      if (this.mounted) {
        this.setState({ almUnboundApplications });
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

  handleCancelImport = () => {
    this.updateUrlQuery({ almInstallId: undefined, almKey: undefined });
  };

  handleOrgCreated = (organization: string) => {
    this.props.skipOnboarding();
    if (this.isStoredTimestampValid(ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP)) {
      this.props.router.push({
        pathname: '/projects/create',
        state: { organization, tab: this.state.almOrganization ? 'auto' : 'manual' }
      });
    } else {
      this.props.router.push({ pathname: getOrganizationUrl(organization) });
    }
  };

  handleOrgDetailsFinish = (organization: T.Organization) => {
    this.setState({ organization, step: Step.Plan });
    return Promise.resolve();
  };

  handleOrgDetailsStepOpen = () => {
    this.setState({ step: Step.OrganizationDetails });
  };

  handlePlanDone = () => {
    if (this.state.organization) {
      this.handleOrgCreated(this.state.organization.key);
    }
  };

  hasAutoImport(state: State): state is StateWithAutoImport {
    return Boolean(state.almApplication);
  }

  isStoredTimestampValid = (timestampKey: string) => {
    const storedTimestamp = get(timestampKey);
    remove(timestampKey);
    return storedTimestamp && differenceInMinutes(Date.now(), Number(storedTimestamp)) < 10;
  };

  onTabChange = (tab: TabKeys) => {
    this.updateUrlState({ tab });
  };

  bindAndRedirectToOrganizationSettings(installationId: string) {
    const organizationKey = get(BIND_ORGANIZATION_KEY) || '';
    remove(BIND_ORGANIZATION_KEY);

    this.setState({ bindingExistingOrg: true });

    bindAlmOrganization({
      installationId,
      organization: organizationKey
    }).then(
      () => {
        this.props.router.push({
          pathname: `/organizations/${organizationKey}`
        });
        addGlobalSuccessMessage(translate('organization.bind.success'));
      },
      () => {}
    );
  }

  getHeader = (bindingExistingOrg: boolean) => {
    if (bindingExistingOrg) {
      return translate('onboarding.binding_organization');
    } else {
      return translate('onboarding.create_organization.page.header');
    }
  };

  setValidOrgKey = (almOrganization: T.AlmOrganization) => {
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

  renderContent = (almInstallId?: string) => {
    const { location } = this.props;
    const { state } = this;
    const { organization, step, subscriptionPlans } = state;
    const { tab = 'auto' } = (location.state || {}) as LocationState;

    const commonProps = {
      handleOrgDetailsFinish: this.handleOrgDetailsFinish,
      handleOrgDetailsStepOpen: this.handleOrgDetailsStepOpen,
      onDone: this.handlePlanDone,
      organization,
      step,
      subscriptionPlans
    };

    if (!this.hasAutoImport(state)) {
      return (
        <ManualOrganizationCreate
          {...commonProps}
          createOrganization={this.props.createOrganization}
          onUpgradeFail={this.deleteOrganization}
          organization={this.state.organization}
          step={this.state.step}
        />
      );
    }

    const { almApplication, almOrganization, boundOrganization } = state;

    return (
      <>
        <Tabs<TabKeys>
          onChange={this.onTabChange}
          selected={tab || 'auto'}
          tabs={[
            {
              key: 'auto',
              node: translateWithParameters(
                'onboarding.import_organization.import_from_x',
                translate(sanitizeAlmId(almApplication.key))
              )
            },
            {
              key: 'manual',
              node: translate('onboarding.create_organization.create_manually')
            }
          ]}
        />

        <ManualOrganizationCreate
          {...commonProps}
          className={classNames({ hidden: tab !== 'manual' && this.hasAutoImport(state) })}
          createOrganization={this.props.createOrganization}
          onUpgradeFail={this.deleteOrganization}
        />

        {almInstallId && almOrganization && !boundOrganization ? (
          <AutoOrganizationCreate
            {...commonProps}
            almApplication={almApplication}
            almInstallId={almInstallId}
            almOrganization={almOrganization}
            className={classNames({ hidden: tab !== 'auto' })}
            createOrganization={this.props.createOrganization}
            handleCancelImport={this.handleCancelImport}
            onOrgCreated={this.handleOrgCreated}
            onUpgradeFail={this.deleteOrganization}
            unboundOrganizations={this.props.userOrganizations.filter(
              ({ actions = {}, alm }) => !alm && actions.admin
            )}
          />
        ) : (
          <RemoteOrganizationChoose
            almApplication={almApplication}
            almInstallId={almInstallId}
            almOrganization={almOrganization}
            almUnboundApplications={state.almUnboundApplications}
            boundOrganization={boundOrganization}
            className={classNames({ hidden: tab !== 'auto' })}
          />
        )}
      </>
    );
  };

  render() {
    const { location } = this.props;
    const query = parseQuery(location.query);

    if (this.state.almOrgLoading) {
      return <AlmApplicationInstalling almKey={query.almKey} />;
    }

    const { bindingExistingOrg, subscriptionPlans } = this.state;
    const header = this.getHeader(bindingExistingOrg);
    const startedPrice = subscriptionPlans && subscriptionPlans[0] && subscriptionPlans[0].price;

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <div className="page page-limited huge-spacer-top huge-spacer-bottom">
          <A11ySkipTarget anchor="create_org_main" />

          <header className="page-header huge-spacer-bottom">
            <h1 className="page-title huge big-spacer-bottom">
              <strong>{header}</strong>
            </h1>
            {startedPrice !== undefined && (
              <p className="page-description">
                {translate('onboarding.create_organization.page.description')}
              </p>
            )}
          </header>
          {this.state.loading ? <DeferredSpinner /> : this.renderContent(query.almInstallId)}
        </div>
      </>
    );
  }
}

const mapDispatchToProps = {
  createOrganization: createOrganization as any,
  deleteOrganization: deleteOrganization as any,
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
