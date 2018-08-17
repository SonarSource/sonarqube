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
import DateFromNow from '../../../components/intl/DateFromNow';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Tooltip from '../../../components/controls/Tooltip';
import { getPeriodDate, getPeriodLabel, Period, PeriodMode } from '../../../helpers/periods';
import { translateWithParameters } from '../../../helpers/l10n';
import { differenceInDays } from '../../../helpers/dates';

interface Props {
  period: Period;
}

export default function LeakPeriodLegend({ period }: Props) {
  const leakPeriodLabel = getPeriodLabel(period);
  if (!leakPeriodLabel) {
    return null;
  }

  if (period.mode === PeriodMode.days) {
    return (
      <div className="overview-legend overview-legend-spaced-line">
        {translateWithParameters('overview.new_code_period_x', leakPeriodLabel)}
      </div>
    );
  }

  const leakPeriodDate = getPeriodDate(period);
  if (!leakPeriodDate) {
    return null;
  }

  const formattedDateFunction = (formattedLeakPeriodDate: string) => (
    <span>
      {translateWithParameters(
        period.mode === PeriodMode.previousAnalysis
          ? 'overview.previous_analysis_on_x'
          : 'overview.started_on_x',
        formattedLeakPeriodDate
      )}
    </span>
  );

  const tooltip =
    differenceInDays(new Date(), leakPeriodDate) < 1 ? (
      <DateTimeFormatter date={leakPeriodDate}>{formattedDateFunction}</DateTimeFormatter>
    ) : (
      <DateFormatter date={leakPeriodDate} long={true}>
        {formattedDateFunction}
      </DateFormatter>
    );

  return (
    <Tooltip overlay={tooltip}>
      <div className="overview-legend">
        {translateWithParameters('overview.new_code_period_x', leakPeriodLabel)}
        <br />
        <DateFromNow date={leakPeriodDate}>
          {fromNow => (
            <span className="note">
              {translateWithParameters(
                period.mode === PeriodMode.previousAnalysis
                  ? 'overview.previous_analysis_x'
                  : 'overview.started_x',
                fromNow
              )}
            </span>
          )}
        </DateFromNow>
      </div>
    </Tooltip>
  );
}
