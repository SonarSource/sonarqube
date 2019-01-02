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
import * as classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import UpgradeOrganizationAdvantages from './UpgradeOrganizationAdvantages';
import RecommendedIcon from '../../../components/icons-components/RecommendedIcon';
import { Alert } from '../../../components/ui/Alert';
import { formatPrice } from '../organization/utils';
import { translate } from '../../../helpers/l10n';
import './CardPlan.css';

interface Props {
  className?: string;
  disabled?: boolean;
  onClick?: () => void;
  selected?: boolean;
  startingPrice?: number;
}

interface CardProps extends Props {
  children: React.ReactNode;
  recommended?: string;
  title: string;
}

export default function CardPlan(props: CardProps) {
  const { className, disabled, onClick, recommended, selected, startingPrice } = props;
  const isActionable = Boolean(onClick);
  return (
    <div
      aria-checked={selected}
      className={classNames(
        'card-plan',
        { 'card-plan-actionable': isActionable, disabled, selected },
        className
      )}
      onClick={isActionable && !disabled ? onClick : undefined}
      role="radio"
      tabIndex={0}>
      <h2 className="card-plan-header big-spacer-bottom">
        <span className="display-flex-center">
          {isActionable && (
            <i className={classNames('icon-radio', 'spacer-right', { 'is-checked': selected })} />
          )}
          {props.title}
        </span>
        {startingPrice !== undefined &&
          (startingPrice ? (
            <FormattedMessage
              defaultMessage={translate('billing.price_from_x')}
              id="billing.price_from_x"
              values={{
                price: <span className="card-plan-price">{formatPrice(startingPrice)}</span>
              }}
            />
          ) : (
            <span className="card-plan-price">{formatPrice(0)}</span>
          ))}
      </h2>
      <div className="card-plan-body">{props.children}</div>
      {recommended && (
        <div className="card-plan-recommended">
          <RecommendedIcon className="spacer-right" />
          <FormattedMessage
            defaultMessage={recommended}
            id={recommended}
            values={{ recommended: <strong>{translate('recommended')}</strong> }}
          />
        </div>
      )}
    </div>
  );
}

interface FreeProps extends Props {
  almName?: string;
  hasWarning: boolean;
}

export function FreeCardPlan({ almName, hasWarning, ...props }: FreeProps) {
  const showInfo = almName && props.disabled;
  const showWarning = almName && hasWarning && !props.disabled;

  return (
    <CardPlan startingPrice={0} title={translate('billing.free_plan.title')} {...props}>
      <>
        <div className="spacer-left">
          <ul className="big-spacer-left note">
            <li className="little-spacer-bottom">
              {translate('billing.free_plan.all_projects_analyzed_public')}
            </li>
            <li>{translate('billing.free_plan.anyone_can_browse_source_code')}</li>
          </ul>
        </div>
        {showWarning && (
          <Alert variant="warning">
            <FormattedMessage
              defaultMessage={translate('billing.free_plan.private_repo_warning')}
              id="billing.free_plan.private_repo_warning"
              values={{ alm: almName }}
            />
          </Alert>
        )}
        {showInfo && (
          <Alert variant="info">
            <FormattedMessage
              defaultMessage={translate('billing.free_plan.not_available_info')}
              id="billing.free_plan.not_available_info"
              values={{ alm: almName }}
            />
          </Alert>
        )}
      </>
    </CardPlan>
  );
}

interface PaidProps extends Props {
  isRecommended: boolean;
}

export function PaidCardPlan({ isRecommended, ...props }: PaidProps) {
  return (
    <CardPlan
      recommended={isRecommended ? translate('billing.paid_plan.recommended') : undefined}
      title={translate('billing.paid_plan.title')}
      {...props}>
      <>
        <UpgradeOrganizationAdvantages />
        <div className="big-spacer-left">
          <Link className="spacer-left" target="_blank" to="/about/pricing">
            {translate('billing.pricing.learn_more')}
          </Link>
        </div>
      </>
    </CardPlan>
  );
}
