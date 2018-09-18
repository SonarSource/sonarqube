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
import BillingFormShim from './BillingFormShim';
import { withCurrentUser } from './withCurrentUser';
import { CurrentUser, SubscriptionPlan } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  createOrganization: () => Promise<string>;
  currentUser: CurrentUser;
  onSubmit: () => void;
  subscriptionPlans: SubscriptionPlan[];
}

export class CardForm extends React.PureComponent<Props> {
  handleClose = () => {
    // do nothing
  };

  render() {
    return (
      <div className="huge-spacer-top">
        <BillingFormShim
          currentUser={this.props.currentUser}
          onClose={this.handleClose}
          onCommit={this.props.onSubmit}
          organizationKey={this.props.createOrganization}
          subscriptionPlans={this.props.subscriptionPlans}>
          {form => (
            <form onSubmit={form.onSubmit}>
              <div className="columns column-show-overflow">
                <div className="column-half">
                  <h3>{translate('billing.upgrade.billing_info')}</h3>
                  {form.renderEmailInput()}
                  {form.renderTypeOfUseSelect()}
                  {form.renderBillingNameInput()}
                  {form.renderCountrySelect()}
                  {form.renderAdditionalInfo()}
                </div>
                <div className="column-half">
                  <h3>{translate('billing.upgrade.plan')}</h3>
                  {form.renderPlanSelect()}
                  <h3>{translate('billing.upgrade.card_info')}</h3>
                  {form.renderBraintreeClient()}
                </div>
              </div>
              <div className="upgrade-footer big-spacer-top">
                {form.renderNextCharge()}
                <hr className="big-spacer-bottom" />
                {form.alertError && <p className="alert alert-danger">{form.alertError}</p>}
              </div>
              <div
                className={classNames({
                  'big-spacer-top': form.alertError !== undefined
                })}>
                {form.renderSpinner()}
                {form.renderSubmitButton(
                  translate('onboarding.create_organization.create_and_upgrade')
                )}
              </div>
              {form.renderTermsOfService()}
            </form>
          )}
        </BillingFormShim>
      </div>
    );
  }
}

export default withCurrentUser(CardForm);
