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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import UpgradeOrganizationAdvantages from './UpgradeOrganizationAdvantages';
import RadioCard, { RadioCardProps } from '../../../components/controls/RadioCard';
import { formatPrice } from '../organization/utils';
import { translate } from '../../../helpers/l10n';

interface Props extends RadioCardProps {
  isRecommended: boolean;
  startingPrice?: number;
}

export default function PaidCardPlan({ isRecommended, startingPrice, ...props }: Props) {
  return (
    <RadioCard
      recommended={isRecommended ? translate('billing.paid_plan.recommended') : undefined}
      title={translate('billing.paid_plan.title')}
      titleInfo={
        startingPrice !== undefined && (
          <FormattedMessage
            defaultMessage={translate('billing.price_from_x')}
            id="billing.price_from_x"
            values={{
              price: <span className="big">{formatPrice(startingPrice)}</span>
            }}
          />
        )
      }
      {...props}>
      <UpgradeOrganizationAdvantages />
      <div className="big-spacer-left">
        <Link className="spacer-left" target="_blank" to="/about/pricing">
          {translate('billing.pricing.learn_more')}
        </Link>
      </div>
    </RadioCard>
  );
}
