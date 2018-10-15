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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import Radio from '../../../components/controls/Radio';
import { translate } from '../../../helpers/l10n';

export enum Plan {
  Free = 'free',
  Paid = 'paid'
}

interface Props {
  onChange: (plan: Plan) => void;
  plan: Plan;
  startingPrice: string;
}

export default class PlanSelect extends React.PureComponent<Props> {
  handleFreePlanClick = () => {
    this.props.onChange(Plan.Free);
  };

  handlePaidPlanClick = () => {
    this.props.onChange(Plan.Paid);
  };

  render() {
    const { plan } = this.props;
    return (
      <div
        aria-label={translate('onboarding.create_organization.choose_plan')}
        className="huge-spacer-bottom"
        role="radiogroup">
        <div>
          <Radio checked={plan === Plan.Free} onCheck={this.handleFreePlanClick}>
            <span>{translate('billing.free_plan.title')}</span>
          </Radio>
          <p className="note markdown little-spacer-top">
            {translate('billing.free_plan.description')}
          </p>
        </div>
        <div className="big-spacer-top">
          <Radio checked={plan === Plan.Paid} onCheck={this.handlePaidPlanClick}>
            <span>{translate('billing.paid_plan.title')}</span>
          </Radio>
          <p className="note markdown little-spacer-top">
            <FormattedMessage
              defaultMessage={translate('billing.paid_plan.description')}
              id="billing.paid_plan.description"
              values={{
                price: this.props.startingPrice,
                more: (
                  <>
                    {' '}
                    <Link target="_blank" to="/documentation/sonarcloud-pricing/">
                      {translate('learn_more')}
                    </Link>
                    <br />
                  </>
                )
              }}
            />
          </p>
        </div>
      </div>
    );
  }
}
