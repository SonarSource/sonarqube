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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import PlanStep from './PlanStep';
import { Step } from './utils';
import { translate } from '../../../helpers/l10n';

interface Props {
  createOrganization: (organization: T.Organization) => Promise<string>;
  className?: string;
  onUpgradeFail: () => void;
  handleOrgDetailsFinish: (organization: T.Organization) => Promise<void>;
  handleOrgDetailsStepOpen: () => void;
  onDone: () => void;
  organization?: T.Organization;
  step: Step;
  subscriptionPlans?: T.SubscriptionPlan[];
}

export default class ManualOrganizationCreate extends React.PureComponent<Props> {
  handleCreateOrganization = () => {
    const { organization } = this.props;
    if (!organization) {
      return Promise.reject();
    }
    return this.props.createOrganization(organization);
  };

  render() {
    const { className, organization, subscriptionPlans } = this.props;
    return (
      <div className={className}>
        <OrganizationDetailsStep
          finished={organization !== undefined}
          onOpen={this.props.handleOrgDetailsStepOpen}
          open={this.props.step === Step.OrganizationDetails}
          organization={organization}>
          <OrganizationDetailsForm
            onContinue={this.props.handleOrgDetailsFinish}
            organization={organization}
            submitText={translate('continue')}
          />
        </OrganizationDetailsStep>

        {subscriptionPlans !== undefined && (
          <PlanStep
            createOrganization={this.handleCreateOrganization}
            onDone={this.props.onDone}
            onUpgradeFail={this.props.onUpgradeFail}
            open={this.props.step === Step.Plan}
            subscriptionPlans={subscriptionPlans}
          />
        )}
      </div>
    );
  }
}
