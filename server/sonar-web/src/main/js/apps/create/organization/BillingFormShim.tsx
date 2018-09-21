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
import { CurrentUser, SubscriptionPlan, Coupon } from '../../../app/types';

interface ChildrenProps {
  alertError: string | undefined;
  couponValue: string;
  onSubmit: React.FormEventHandler;
  renderAdditionalInfo: () => React.ReactNode;
  renderBillingNameInput: () => React.ReactNode;
  renderBraintreeClient: () => React.ReactNode;
  renderCountrySelect: () => React.ReactNode;
  renderCouponInput: (children?: React.ReactNode) => React.ReactNode;
  renderEmailInput: () => React.ReactNode;
  renderNextCharge: () => React.ReactNode;
  renderPlanSelect: () => React.ReactNode;
  renderResetButton: () => React.ReactNode;
  renderSpinner: () => React.ReactNode;
  renderSubmitButton: (text?: string) => React.ReactNode;
  renderTermsOfService: () => React.ReactNode;
  renderTypeOfUseSelect: () => React.ReactNode;
}

interface Props {
  children: (props: ChildrenProps) => React.ReactElement<any>;
  country?: string;
  currentUser: CurrentUser;
  onClose: () => void;
  onCommit: () => void;
  onCouponUpdate?: (coupon?: Coupon) => void;
  onFailToUpgrade?: () => void;
  organizationKey: string | (() => Promise<string>);
  skipBraintreeInit?: boolean;
  subscriptionPlans: SubscriptionPlan[];
}

export default class BillingFormShim extends React.Component<Props> {
  render() {
    const { BillingForm } = (window as any).SonarBilling;
    return <BillingForm {...this.props} />;
  }
}
