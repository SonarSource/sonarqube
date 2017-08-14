/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import {
  formatMeasure,
  formatMeasureVariation,
  getRatingTooltip as nextGetRatingTooltip,
  isDiffMetric
} from '../../helpers/measures';
import type { Measure, MeasureEnhanced } from './types';
import type { Metric } from '../../store/metrics/actions';

const KNOWN_RATINGS = ['sqale_rating', 'reliability_rating', 'security_rating'];

export const enhanceMeasure = (
  measure: Measure,
  metrics: { [string]: Metric }
): MeasureEnhanced => ({
  value: measure.value,
  periods: measure.periods,
  metric: metrics[measure.metric],
  leak: getLeakValue(measure)
});

export function formatLeak(value: ?string, metric: Metric, options: Object) {
  if (isDiffMetric(metric.key)) {
    return formatMeasure(value, metric.type, options);
  } else {
    return formatMeasureVariation(value, metric.type, options);
  }
}

export function getLeakValue(measure: ?Measure): ?string {
  if (!measure || !measure.periods) {
    return null;
  }
  const period = measure.periods.find(period => period.index === 1);
  return period ? period.value : null;
}

export function getRatingTooltip(metricKey: string, value: ?string) {
  const finalMetricKey = isDiffMetric(metricKey) ? metricKey.substr(4) : metricKey;
  if (KNOWN_RATINGS.includes(finalMetricKey)) {
    return nextGetRatingTooltip(finalMetricKey, value);
  }
  return null;
}
