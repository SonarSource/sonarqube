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
import { getLeakValue } from '../../../components/measure/utils';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';

export interface DebtValueProps {
  branchLike?: BranchLike;
  component: Component;
  measures: MeasureEnhanced[];
  useDiffMetric?: boolean;
}

export function DebtValue(props: DebtValueProps) {
  const { branchLike, component, measures, useDiffMetric = false } = props;
  const metricKey = useDiffMetric ? MetricKey.new_technical_debt : MetricKey.sqale_index;
  const measure = findMeasure(measures, metricKey);

  let value;
  let metricName;
  if (measure) {
    value = useDiffMetric ? getLeakValue(measure) : measure.value;
    metricName = getLocalizedMetricName(measure.metric, true);
  } else {
    metricName = localizeMetric(metricKey);
  }
  const formattedValue = formatMeasure(value, 'WORK_DUR');

  return (
    <>
      {value === undefined ? (
        <span aria-label={translate('no_data')} className="overview-measures-empty-value" />
      ) : (
        <DrilldownLink
          ariaLabel={translateWithParameters(
            'overview.see_more_details_on_x_of_y',
            formattedValue,
            metricName
          )}
          branchLike={branchLike}
          className="overview-measures-value text-light"
          component={component.key}
          metric={metricKey}
        >
          {formattedValue}
        </DrilldownLink>
      )}
      <span className="big-spacer-left">{metricName}</span>
    </>
  );
}

export default React.memo(DebtValue);
