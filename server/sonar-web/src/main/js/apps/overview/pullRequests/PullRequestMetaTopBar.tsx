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
import { SeparatorCircleIcon } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import CurrentBranchLikeMergeInformation from '../../../app/components/nav/component/branch-like/CurrentBranchLikeMergeInformation';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getLeakValue } from '../../../components/measure/utils';
import { findMeasure, formatMeasure } from '../../../helpers/measures';
import { PullRequest } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { MeasureEnhanced } from '../../../types/types';

interface Props {
  pullRequest: PullRequest;
  measures: MeasureEnhanced[];
}

export default function PullRequestMetaTopBar({ pullRequest, measures }: Readonly<Props>) {
  const intl = useIntl();

  const leftSection = (
    <div>
      <strong className="sw-body-sm-highlight sw-mr-1">
        {formatMeasure(
          getLeakValue(findMeasure(measures, MetricKey.new_lines)),
          MetricType.ShortInteger,
        ) || '0'}
      </strong>
      {intl.formatMessage({ id: 'metric.new_lines.name' })}
    </div>
  );
  const rightSection = (
    <div className="sw-flex sw-items-center sw-gap-2">
      <CurrentBranchLikeMergeInformation pullRequest={pullRequest} />

      {pullRequest.analysisDate && (
        <>
          <SeparatorCircleIcon />
          {intl.formatMessage(
            {
              id: 'overview.last_analysis_x',
            },
            {
              date: (
                <strong className="sw-body-sm-highlight">
                  <DateFromNow date={pullRequest.analysisDate} />
                </strong>
              ),
            },
          )}
        </>
      )}
    </div>
  );

  return (
    <div className="sw-flex sw-justify-between sw-whitespace-nowrap sw-body-sm">
      {leftSection}
      {rightSection}
    </div>
  );
}
