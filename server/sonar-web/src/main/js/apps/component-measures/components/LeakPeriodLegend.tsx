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
import DateFromNow from '../../../components/intl/DateFromNow';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Tooltip from '../../../components/controls/Tooltip';
import { getPeriodLabel, getPeriodDate, Period, PeriodMode } from '../../../helpers/periods';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { differenceInDays } from '../../../helpers/dates';
import { ComponentMeasure } from '../../../app/types';

interface Props {
  className?: string;
  component: ComponentMeasure;
  period: Period;
}

export default function LeakPeriodLegend({ className, component, period }: Props) {
  const leakClass = classNames('domain-measures-leak-header', className);
  if (component.qualifier === 'APP') {
    return <div className={leakClass}>{translate('issues.new_code_period')}</div>;
  }

  const leakPeriodLabel = getPeriodLabel(period);
  if (!leakPeriodLabel) {
    return null;
  }

  const label = (
    <div className={leakClass}>
      {translateWithParameters('overview.new_code_period_x', leakPeriodLabel)}
    </div>
  );

  if (period.mode === PeriodMode.days) {
    return label;
  }

  const date = getPeriodDate(period);
  const tooltip = date && (
    <div>
      <DateFromNow date={date} />
      {', '}
      {differenceInDays(new Date(), date) < 1 ? (
        <DateTimeFormatter date={date} />
      ) : (
        <DateFormatter date={date} long={true} />
      )}
    </div>
  );

  return <Tooltip overlay={tooltip}>{label}</Tooltip>;
}
