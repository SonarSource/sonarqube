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
import { CurrentUser, SubscriptionPlan } from '../../../app/types';

interface ChildrenProps {
  onSubmit: React.FormEventHandler;
  renderFormFields: () => React.ReactElement<any>;
  renderSubmitGroup: (submitText?: string) => React.ReactElement<any>;
}

interface Props {
  children: (props: ChildrenProps) => React.ReactElement<any>;
  initialCountry?: string;
  currentUser: CurrentUser;
  onCommit: () => void;
  onFailToUpgrade?: () => void;
  organizationKey: string | (() => Promise<string>);
  subscriptionPlans: SubscriptionPlan[];
}

export default class BillingFormShim extends React.Component<Props> {
  render() {
    const { BillingForm } = (window as any).SonarBilling;
    return <BillingForm {...this.props} />;
  }
}
