/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import { formatterOption } from '../../../components/intl/DateTimeFormatter';
import { translateWithParameters } from '../../../helpers/l10n';
import { getNewCodePeriodDate, getNewCodePeriodLabel } from '../../../helpers/new-code-period';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Period } from '../../../types/types';

export interface ProjectLeakPeriodInfoProps extends WrappedComponentProps {
  leakPeriod: Period;
}

export function ProjectLeakPeriodInfo(props: ProjectLeakPeriodInfoProps) {
  const {
    intl: { formatDate, formatTime },
    leakPeriod,
  } = props;

  const leakPeriodLabel = getNewCodePeriodLabel(
    leakPeriod,
    ['manual_baseline', NewCodeDefinitionType.SpecificAnalysis].includes(leakPeriod.mode)
      ? (date: string) => formatTime(date, formatterOption)
      : (date: string) => formatDate(date, longFormatterOption),
  );

  if (!leakPeriodLabel) {
    return null;
  }

  if (
    leakPeriod.mode === 'days' ||
    leakPeriod.mode === NewCodeDefinitionType.NumberOfDays ||
    leakPeriod.mode === NewCodeDefinitionType.ReferenceBranch
  ) {
    return <div>{leakPeriodLabel} </div>;
  }

  const leakPeriodDate = getNewCodePeriodDate(leakPeriod);

  if (!leakPeriodDate) {
    return null;
  }

  return (
    <>
      <div className="sw-mr-2 sw-text-ellipsis" title={leakPeriodLabel}>
        {leakPeriodLabel}
      </div>

      <DateFromNow date={leakPeriodDate}>
        {(fromNow) =>
          translateWithParameters(
            leakPeriod.mode === 'previous_analysis'
              ? 'overview.previous_analysis_x'
              : 'overview.started_x',
            fromNow,
          )
        }
      </DateFromNow>
    </>
  );
}

export default React.memo(injectIntl(ProjectLeakPeriodInfo));
