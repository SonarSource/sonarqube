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
import PaymentMethodSelect, { PaymentMethod } from './PaymentMethodSelect';
import CardForm from './CardForm';
import CouponForm from './CouponForm';
import PlanSelect, { Plan } from './PlanSelect';
import Step from '../../tutorials/components/Step';
import { translate } from '../../../helpers/l10n';
import { getExtensionStart } from '../../../app/components/extensions/utils';
import { SubscriptionPlan } from '../../../app/types';
import { SubmitButton } from '../../../components/ui/buttons';
import DeferredSpinner from '../../../components/common/DeferredSpinner';

interface Props {
  createOrganization: () => Promise<string>;
  deleteOrganization: () => void;
  onFreePlanChoose: () => Promise<void>;
  onPaidPlanChoose: () => void;
  onlyPaid?: boolean;
  open: boolean;
  startingPrice: string;
  subscriptionPlans: SubscriptionPlan[];
}

interface State {
  paymentMethod?: PaymentMethod;
  plan: Plan;
  ready: boolean;
  submitting: boolean;
}

export default class PlanStep extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      plan: props.onlyPaid ? Plan.Paid : Plan.Free,
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

  handlePaymentMethodChange = (paymentMethod: PaymentMethod) => {
    this.setState({ paymentMethod });
  };

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  handleFreePlanSubmit = () => {
    this.setState({ submitting: true });
    this.props.onFreePlanChoose().then(this.stopSubmitting, this.stopSubmitting);
  };

  renderForm = () => {
    return (
      <div className="boxed-group-inner">
        {this.state.ready && (
          <>
            {!this.props.onlyPaid && (
              <PlanSelect
                onChange={this.handlePlanChange}
                plan={this.state.plan}
                startingPrice={this.props.startingPrice}
              />
            )}

            {this.state.plan === Plan.Paid ? (
              <>
                <PaymentMethodSelect
                  onChange={this.handlePaymentMethodChange}
                  paymentMethod={this.state.paymentMethod}
                />
                {this.state.paymentMethod === PaymentMethod.Card && (
                  <CardForm
                    createOrganization={this.props.createOrganization}
                    onFailToUpgrade={this.props.deleteOrganization}
                    onSubmit={this.props.onPaidPlanChoose}
                    subscriptionPlans={this.props.subscriptionPlans}
                  />
                )}
                {this.state.paymentMethod === PaymentMethod.Coupon && (
                  <CouponForm
                    createOrganization={this.props.createOrganization}
                    onFailToUpgrade={this.props.deleteOrganization}
                    onSubmit={this.props.onPaidPlanChoose}
                  />
                )}
              </>
            ) : (
              <div className="display-flex-center big-spacer-top">
                <SubmitButton disabled={this.state.submitting} onClick={this.handleFreePlanSubmit}>
                  {translate('my_account.create_organization')}
                </SubmitButton>
                {this.state.submitting && <DeferredSpinner className="spacer-left" />}
              </div>
            )}
          </>
        )}
      </div>
    );
  };

  render() {
    const stepTitle = translate(
      this.props.onlyPaid
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
