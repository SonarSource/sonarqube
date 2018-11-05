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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import PlanStep from './PlanStep';
import { formatPrice } from './utils';
import { OrganizationBase, Organization, SubscriptionPlan } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  createOrganization: (organization: OrganizationBase) => Promise<Organization>;
  className?: string;
  deleteOrganization: (key: string) => Promise<void>;
  onOrgCreated: (organization: string) => void;
  onlyPaid?: boolean;
  subscriptionPlans?: SubscriptionPlan[];
}

enum Step {
  OrganizationDetails,
  Plan
}

interface State {
  organization?: Organization;
  step: Step;
}

export default class ManualOrganizationCreate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { step: Step.OrganizationDetails };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleOrganizationDetailsStepOpen = () => {
    this.setState({ step: Step.OrganizationDetails });
  };

  handleOrganizationDetailsFinish = (organization: Required<OrganizationBase>) => {
    this.setState({ organization, step: Step.Plan });
    return Promise.resolve();
  };

  handlePaidPlanChoose = () => {
    if (this.state.organization) {
      this.props.onOrgCreated(this.state.organization.key);
    }
  };

  handleFreePlanChoose = () => {
    return this.createOrganization().then(key => {
      this.props.onOrgCreated(key);
    });
  };

  createOrganization = () => {
    const { organization } = this.state;
    if (organization) {
      return this.props
        .createOrganization({
          avatar: organization.avatar,
          description: organization.description,
          key: organization.key,
          name: organization.name || organization.key,
          url: organization.url
        })
        .then(({ key }) => key);
    } else {
      return Promise.reject();
    }
  };

  deleteOrganization = () => {
    const { organization } = this.state;
    if (organization) {
      this.props.deleteOrganization(organization.key).catch(() => {});
    }
  };

  render() {
    const { className, subscriptionPlans } = this.props;
    const startedPrice = subscriptionPlans && subscriptionPlans[0] && subscriptionPlans[0].price;
    const formattedPrice = formatPrice(startedPrice);

    return (
      <div className={className}>
        <OrganizationDetailsStep
          finished={this.state.organization !== undefined}
          onOpen={this.handleOrganizationDetailsStepOpen}
          open={this.state.step === Step.OrganizationDetails}
          organization={this.state.organization}>
          <OrganizationDetailsForm
            onContinue={this.handleOrganizationDetailsFinish}
            organization={this.state.organization}
            submitText={translate('continue')}
          />
        </OrganizationDetailsStep>

        {subscriptionPlans !== undefined && (
          <PlanStep
            createOrganization={this.createOrganization}
            deleteOrganization={this.deleteOrganization}
            onFreePlanChoose={this.handleFreePlanChoose}
            onPaidPlanChoose={this.handlePaidPlanChoose}
            onlyPaid={this.props.onlyPaid}
            open={this.state.step === Step.Plan}
            startingPrice={formattedPrice}
            subscriptionPlans={subscriptionPlans}
          />
        )}
      </div>
    );
  }
}
