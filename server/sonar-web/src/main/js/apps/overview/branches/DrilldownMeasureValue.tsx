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
import { DrilldownLink } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
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

  if (!measure || measure.value === undefined) {
    return <span>–</span>;
  }

  const url = getComponentDrilldownUrl({
    branchLike,
    componentKey: component.key,
    metric,
  });

  return (
    <span>
      <DrilldownLink
        aria-label={translateWithParameters(
          'overview.see_more_details_on_x_y',
          measure.value,
          localizeMetric(metric),
        )}
        to={url}
      >
        {formatMeasure(measure.value, MetricType.ShortInteger)}
      </DrilldownLink>
    </span>
  );
}

export default React.memo(DrilldownMeasureValue);
