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
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getLocalizedMetricName, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';

export interface DrilldownMeasureValueProps {
  branchLike?: BranchLike;
  component: Component;
  measures: MeasureEnhanced[];
  metric: MetricKey;
}

export function DrilldownMeasureValue(props: DrilldownMeasureValueProps) {
  const { branchLike, component, measures, metric } = props;
  const measure = findMeasure(measures, metric);

  let content;
  if (!measure || measure.value === undefined) {
    content = <span className="overview-measures-value text-light">-</span>;
  } else {
    content = (
      <span>
        <DrilldownLink
          ariaLabel={translateWithParameters(
            'overview.see_more_details_on_x_y',
            measure.value,
            localizeMetric(metric)
          )}
          branchLike={branchLike}
          className="overview-measures-value text-light"
          component={component.key}
          metric={metric}
        >
          {formatMeasure(measure.value, 'SHORT_INT')}
        </DrilldownLink>
      </span>
    );
  }

  return (
    <div className="display-flex-column display-flex-center">
      {content}
      <span className="spacer-top">{getLocalizedMetricName({ key: metric })}</span>
    </div>
  );
}

export default React.memo(DrilldownMeasureValue);
