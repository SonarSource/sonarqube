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
import { CurrentUser, Coupon } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';

interface Props {
  createOrganization: () => Promise<string>;
  currentUser: CurrentUser;
  onSubmit: () => void;
}

interface State {
  coupon?: Coupon;
}

export class CouponForm extends React.PureComponent<Props, State> {
  state: State = {};

  handleClose = () => {
    // do nothing
  };

  handleCouponUpdate = (coupon?: Coupon) => {
    this.setState({ coupon });
  };

  renderBillingInformation() {
    if (!this.state.coupon || !this.state.coupon.billing) {
      return null;
    }
    const { billing } = this.state.coupon;
    return (
      <div className="big-spacer-top big-spacer-bottom" id="coupon-billing-information">
        <h3 className="note">{translate('billing.upgrade.billing_info')}</h3>
        <ul className="note">
          {Boolean(billing.name) && <li>{billing.name}</li>}
          {Boolean(billing.address) && <li>{billing.address}</li>}
          {Boolean(billing.country) && <li>{billing.country}</li>}
          {Boolean(billing.email) && <li>{billing.email}</li>}
        </ul>
      </div>
    );
  }

  render() {
    return (
      <div className="huge-spacer-top">
        <BillingFormShim
          currentUser={this.props.currentUser}
          onClose={this.handleClose}
          onCommit={this.props.onSubmit}
          onCouponUpdate={this.handleCouponUpdate}
          organizationKey={this.props.createOrganization}
          skipBraintreeInit={true}
          subscriptionPlans={[]}>
          {form => (
            <form onSubmit={form.onSubmit}>
              {form.renderCouponInput(
                <label htmlFor="coupon">
                  {translate('billing.upgrade.coupon')}
                  <DocTooltip
                    className="little-spacer-left"
                    doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/billing/coupon.md')}
                  />
                </label>
              )}
              {this.renderBillingInformation()}
              {this.state.coupon &&
                !this.state.coupon.billing && (
                  <>
                    <h3 className="big-spacer-top">{translate('billing.upgrade.billing_info')}</h3>
                    {form.renderEmailInput()}
                    {form.renderTypeOfUseSelect()}
                    {form.renderBillingNameInput()}
                    {form.renderCountrySelect()}
                    {form.renderAdditionalInfo()}
                  </>
                )}
              {this.state.coupon && (
                <div className="big-spacer-bottom">{form.renderNextCharge()}</div>
              )}
              {form.alertError && <p className="alert alert-danger">{form.alertError}</p>}
              <div className={classNames({ 'big-spacer-top': form.alertError !== undefined })}>
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

export default withCurrentUser(CouponForm);
