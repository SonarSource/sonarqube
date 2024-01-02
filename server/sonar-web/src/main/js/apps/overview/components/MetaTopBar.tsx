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
import React from 'react';
import { useIntl } from 'react-intl';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getLeakValue } from '../../../components/measure/utils';
import { isPullRequest } from '../../../helpers/branch-like';
import { findMeasure, formatMeasure } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { MeasureEnhanced } from '../../../types/types';

interface Props {
  branchLike: BranchLike;
  measures: MeasureEnhanced[];
}

export default function MetaTopBar({ branchLike, measures }: Readonly<Props>) {
  const intl = useIntl();
  const isPR = isPullRequest(branchLike);

  const leftSection = (
    <div>
      {isPR ? (
        <>
          <strong className="sw-body-sm-highlight sw-mr-1">
            {formatMeasure(
              getLeakValue(findMeasure(measures, MetricKey.new_lines)),
              MetricType.ShortInteger,
            ) ?? '0'}
          </strong>
          {intl.formatMessage({ id: 'metric.new_lines.name' })}
        </>
      ) : null}
    </div>
  );
  const rightSection = (
    <div>
      {branchLike.analysisDate
        ? intl.formatMessage(
            {
              id: 'overview.last_analysis_x',
            },
            {
              date: (
                <strong className="sw-body-sm-highlight">
                  <DateFromNow date={branchLike.analysisDate} />
                </strong>
              ),
            },
          )
        : null}
    </div>
  );

  return (
    <div className="sw-flex sw-justify-between sw-whitespace-nowrap sw-body-sm">
      {leftSection}
      {rightSection}
    </div>
  );
}
