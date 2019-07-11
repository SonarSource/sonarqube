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
import { translate } from 'sonar-ui-common/helpers/l10n';
import FreeCardPlan from '../components/FreeCardPlan';
import PaidCardPlan from '../components/PaidCardPlan';

export enum Plan {
  Free = 'free',
  Paid = 'paid'
}

interface Props {
  almApplication?: T.AlmApplication;
  almOrganization?: T.AlmOrganization;
  onChange: (plan: Plan) => void;
  plan: Plan;
  startingPrice: number;
}

export default class PlanSelect extends React.PureComponent<Props> {
  handleFreePlanClick = () => {
    this.props.onChange(Plan.Free);
  };

  handlePaidPlanClick = () => {
    this.props.onChange(Plan.Paid);
  };

  render() {
    const { almApplication, almOrganization, plan } = this.props;
    const hasPrivateRepo = Boolean(almOrganization && almOrganization.privateRepos > 0);
    const onlyPrivateRepo = Boolean(
      hasPrivateRepo && almOrganization && almOrganization.publicRepos === 0
    );

    const cards = [
      <PaidCardPlan
        isRecommended={hasPrivateRepo}
        key="paid"
        onClick={this.handlePaidPlanClick}
        selected={plan === Plan.Paid}
        startingPrice={this.props.startingPrice}
      />,
      <FreeCardPlan
        almName={almApplication && almApplication.name}
        disabled={onlyPrivateRepo}
        hasWarning={hasPrivateRepo && plan === Plan.Free}
        key="free"
        onClick={this.handleFreePlanClick}
        selected={plan === Plan.Free}
      />
    ];

    return (
      <div
        aria-label={translate('onboarding.create_organization.choose_plan')}
        className="display-flex-row huge-spacer-bottom"
        role="radiogroup">
        {hasPrivateRepo ? cards : cards.reverse()}
      </div>
    );
  }
}
