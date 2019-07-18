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
import * as differenceInDays from 'date-fns/difference_in_days';
import * as React from 'react';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateFormatter, { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter, { formatterOption } from '../../../components/intl/DateTimeFormatter';
import { getPeriodDate, getPeriodLabel } from '../../../helpers/periods';

interface Props {
  period: T.Period;
}

export class LeakPeriodLegend extends React.PureComponent<Props & InjectedIntlProps> {
  formatDate = (date: string) => {
    return this.props.intl.formatDate(date, longFormatterOption);
  };

  formatDateTime = (date: string) => {
    return this.props.intl.formatTime(date, formatterOption);
  };

  render() {
    const { period } = this.props;
    const leakPeriodLabel = getPeriodLabel(
      period,
      period.mode === 'manual_baseline' ? this.formatDateTime : this.formatDate
    );
    if (!leakPeriodLabel) {
      return null;
    }

    if (period.mode === 'days') {
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
          period.mode === 'previous_analysis'
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
                  period.mode === 'previous_analysis'
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
}

export default injectIntl(LeakPeriodLegend);
