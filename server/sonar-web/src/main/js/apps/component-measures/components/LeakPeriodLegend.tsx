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
import * as classNames from 'classnames';
import * as differenceInDays from 'date-fns/difference_in_days';
import * as React from 'react';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateFormatter, { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter, { formatterOption } from '../../../components/intl/DateTimeFormatter';
import { getPeriodDate, getPeriodLabel } from '../../../helpers/periods';

interface Props {
  className?: string;
  component: T.ComponentMeasure;
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
    const { className, component, period } = this.props;
    const leakClass = classNames('domain-measures-header leak-box', className);
    if (component.qualifier === 'APP') {
      return <div className={leakClass}>{translate('issues.new_code_period')}</div>;
    }

    const leakPeriodLabel = getPeriodLabel(
      period,
      period.mode === 'manual_baseline' ? this.formatDateTime : this.formatDate
    );
    if (!leakPeriodLabel) {
      return null;
    }

    const label = (
      <div className={leakClass}>
        {translateWithParameters('overview.new_code_period_x', leakPeriodLabel)}
      </div>
    );

    if (period.mode === 'days') {
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
}

export default injectIntl(LeakPeriodLegend);
