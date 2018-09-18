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
import RadioToggle from '../../../components/controls/RadioToggle';
import { translate } from '../../../helpers/l10n';

export enum PaymentMethod {
  Card = 'card',
  Coupon = 'coupon'
}

interface Props {
  onChange: (paymentMethod: PaymentMethod) => void;
  paymentMethod: PaymentMethod | undefined;
}

export default class PaymentMethodSelect extends React.PureComponent<Props> {
  render() {
    const options = Object.values(PaymentMethod).map(value => ({
      label: translate('billing', value),
      value
    }));

    return (
      <div>
        <label className="spacer-bottom">
          {translate('onboarding.create_organization.choose_payment_method')}
        </label>
        <div className="little-spacer-top">
          <RadioToggle
            name="payment-method"
            onCheck={this.props.onChange}
            options={options}
            value={this.props.paymentMethod}
          />
        </div>
      </div>
    );
  }
}
