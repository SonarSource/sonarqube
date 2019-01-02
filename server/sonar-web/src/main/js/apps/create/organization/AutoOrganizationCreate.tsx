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
import { FormattedMessage } from 'react-intl';
import AutoOrganizationBind from './AutoOrganizationBind';
import OrganizationDetailsForm from './OrganizationDetailsForm';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import PlanStep from './PlanStep';
import { Step } from './utils';
import { DeleteButton } from '../../../components/ui/buttons';
import RadioToggle from '../../../components/controls/RadioToggle';
import { bindAlmOrganization } from '../../../api/alm-integration';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

enum Filters {
  Bind = 'bind',
  Create = 'create'
}

interface Props {
  almApplication: T.AlmApplication;
  almInstallId: string;
  almOrganization: T.AlmOrganization;
  className?: string;
  createOrganization: (
    organization: T.Organization & { installationId?: string }
  ) => Promise<string>;
  handleCancelImport: () => void;
  handleOrgDetailsFinish: (organization: T.Organization) => Promise<void>;
  handleOrgDetailsStepOpen: () => void;
  onDone: () => void;
  onOrgCreated: (organization: string) => void;
  onUpgradeFail: () => void;
  organization?: T.Organization;
  step: Step;
  subscriptionPlans?: T.SubscriptionPlan[];
  unboundOrganizations: T.Organization[];
}

interface State {
  filter?: Filters;
}

export default class AutoOrganizationCreate extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      filter: props.unboundOrganizations.length === 0 ? Filters.Create : undefined
    };
  }

  handleBindOrganization = (organization: string) => {
    return bindAlmOrganization({
      organization,
      installationId: this.props.almInstallId
    }).then(() => this.props.onOrgCreated(organization));
  };

  handleCreateOrganization = () => {
    const { organization } = this.props;
    if (!organization) {
      return Promise.reject();
    }
    return this.props.createOrganization({
      ...organization,
      installationId: this.props.almInstallId
    });
  };

  handleOptionChange = (filter: Filters) => {
    this.setState({ filter });
  };

  render() {
    const {
      almApplication,
      almOrganization,
      className,
      organization,
      step,
      subscriptionPlans,
      unboundOrganizations
    } = this.props;
    const { filter } = this.state;
    const hasUnboundOrgs = unboundOrganizations.length > 0;
    return (
      <div className={className}>
        <OrganizationDetailsStep
          finished={organization !== undefined}
          onOpen={this.props.handleOrgDetailsStepOpen}
          open={step === Step.OrganizationDetails}
          organization={organization}
          stepTitle={translate('onboarding.import_organization.import_org_details')}>
          <div className="huge-spacer-bottom">
            <p className="display-flex-center big-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('onboarding.import_organization_x')}
                id="onboarding.import_organization_x"
                values={{
                  avatar: (
                    <img
                      alt={almApplication.name}
                      className="little-spacer-left"
                      src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(
                        almApplication.key
                      )}.svg`}
                      width={16}
                    />
                  ),
                  name: <strong>{almOrganization.name}</strong>
                }}
              />
              <DeleteButton
                className="little-spacer-left"
                onClick={this.props.handleCancelImport}
              />
            </p>

            {hasUnboundOrgs && (
              <RadioToggle
                name="filter"
                onCheck={this.handleOptionChange}
                options={[
                  {
                    label: translate('onboarding.import_organization.create_new'),
                    value: Filters.Create
                  },
                  {
                    label: translate('onboarding.import_organization.bind_existing'),
                    value: Filters.Bind
                  }
                ]}
                value={filter}
              />
            )}
          </div>

          {filter === Filters.Create && (
            <OrganizationDetailsForm
              onContinue={this.props.handleOrgDetailsFinish}
              organization={almOrganization}
              submitText={translate('continue')}
            />
          )}
          {filter === Filters.Bind && (
            <AutoOrganizationBind
              onBindOrganization={this.handleBindOrganization}
              unboundOrganizations={unboundOrganizations}
            />
          )}
        </OrganizationDetailsStep>

        {subscriptionPlans !== undefined &&
          filter !== Filters.Bind && (
            <PlanStep
              almApplication={this.props.almApplication}
              almOrganization={this.props.almOrganization}
              createOrganization={this.handleCreateOrganization}
              onDone={this.props.onDone}
              onUpgradeFail={this.props.onUpgradeFail}
              open={step === Step.Plan}
              subscriptionPlans={subscriptionPlans}
            />
          )}
      </div>
    );
  }
}
