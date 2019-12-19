/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { InjectedIntl, injectIntl } from 'react-intl';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import { formatterOption } from '../../../components/intl/DateTimeFormatter';
import { getPeriodDate, getPeriodLabel } from '../../../helpers/periods';

export interface ProjectLeakPeriodInfoProps {
  intl: Pick<InjectedIntl, 'formatDate' | 'formatTime'>;
  leakPeriod: T.Period;
}

export function ProjectLeakPeriodInfo(props: ProjectLeakPeriodInfoProps) {
  const {
    intl: { formatDate, formatTime },
    leakPeriod
  } = props;

  const leakPeriodLabel = getPeriodLabel(
    leakPeriod,
    ['manual_baseline', 'SPECIFIC_ANALYSIS'].includes(leakPeriod.mode)
      ? (date: string) => formatTime(date, formatterOption)
      : (date: string) => formatDate(date, longFormatterOption)
  );

  if (!leakPeriodLabel) {
    return null;
  }

  if (leakPeriod.mode === 'days' || leakPeriod.mode === 'NUMBER_OF_DAYS') {
    return <div className="note spacer-top">{leakPeriodLabel} </div>;
  }

  const leakPeriodDate = getPeriodDate(leakPeriod);

  if (!leakPeriodDate) {
    return null;
  }

  return (
    <>
      <div className="note spacer-top">{leakPeriodLabel}</div>
      <DateFromNow date={leakPeriodDate}>
        {fromNow => (
          <div className="note little-spacer-top">
            {translateWithParameters(
              leakPeriod.mode === 'previous_analysis'
                ? 'overview.previous_analysis_x'
                : 'overview.started_x',
              fromNow
            )}
          </div>
        )}
      </DateFromNow>
    </>
  );
}

export default React.memo(injectIntl(ProjectLeakPeriodInfo));
