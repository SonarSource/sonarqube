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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import { getExtensionStart } from '../../../helpers/extensions';
import Step from '../../tutorials/components/Step';
import BillingFormShim from '../components/BillingFormShim';
import PlanSelect, { Plan } from './PlanSelect';

const BillingForm = withCurrentUser(BillingFormShim);

interface Props {
  almApplication?: T.AlmApplication;
  almOrganization?: T.AlmOrganization;
  createOrganization: () => Promise<string>;
  onDone: () => void;
  onUpgradeFail?: () => void;
  open: boolean;
  subscriptionPlans: T.SubscriptionPlan[];
}

interface State {
  plan: Plan;
  ready: boolean;
  submitting: boolean;
}

export default class PlanStep extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      plan: props.almOrganization && props.almOrganization.privateRepos > 0 ? Plan.Paid : Plan.Free,
      ready: false,
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    getExtensionStart('billing/billing').then(
      () => {
        if (this.mounted) {
          this.setState({ ready: true });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handlePlanChange = (plan: Plan) => {
    this.setState({ plan });
  };

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  handleFreePlanSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    this.setState({ submitting: true });
    return this.props.createOrganization().then(() => {
      this.props.onDone();
      this.stopSubmitting();
    }, this.stopSubmitting);
  };

  renderForm = () => {
    const { submitting } = this.state;
    const { subscriptionPlans } = this.props;
    const startingPrice = subscriptionPlans && subscriptionPlans[0] && subscriptionPlans[0].price;
    return (
      <div className="boxed-group-inner">
        {this.state.ready && (
          <>
            <PlanSelect
              almApplication={this.props.almApplication}
              almOrganization={this.props.almOrganization}
              onChange={this.handlePlanChange}
              plan={this.state.plan}
              startingPrice={startingPrice}
            />

            {this.state.plan === Plan.Paid ? (
              <BillingForm
                onCommit={this.props.onDone}
                onFailToUpgrade={this.props.onUpgradeFail}
                organizationKey={this.props.createOrganization}
                subscriptionPlans={this.props.subscriptionPlans}>
                {({ onSubmit, renderFormFields, renderSubmitGroup }) => (
                  <form id="organization-paid-plan-form" onSubmit={onSubmit}>
                    {renderFormFields()}
                    <div className="billing-input-large big-spacer-top">
                      {renderSubmitGroup(
                        translate('onboarding.create_organization.create_and_upgrade')
                      )}
                    </div>
                  </form>
                )}
              </BillingForm>
            ) : (
              <form
                className="display-flex-center big-spacer-top"
                id="organization-free-plan-form"
                onSubmit={this.handleFreePlanSubmit}>
                <SubmitButton disabled={submitting}>
                  {translate('my_account.create_organization')}
                </SubmitButton>
                {submitting && <DeferredSpinner className="spacer-left" />}
              </form>
            )}
          </>
        )}
      </div>
    );
  };

  render() {
    const { almOrganization } = this.props;
    const stepTitle = translate(
      almOrganization && almOrganization.privateRepos > 0 && almOrganization.publicRepos === 0
        ? 'onboarding.create_organization.enter_payment_details'
        : 'onboarding.create_organization.choose_plan'
    );

    return (
      <Step
        finished={false}
        onOpen={() => {}}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={() => null}
        stepNumber={2}
        stepTitle={stepTitle}
      />
    );
  }
}
