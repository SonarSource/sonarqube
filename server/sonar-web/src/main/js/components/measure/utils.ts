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
import { getRatingTooltip as nextGetRatingTooltip, isDiffMetric } from '../../helpers/measures';
import { getLeakPeriod } from '../../helpers/periods';

const KNOWN_RATINGS = [
  'sqale_rating',
  'maintainability_rating', // Needed to provide the label for "new_maintainability_rating"
  'reliability_rating',
  'security_rating'
];

export function enhanceMeasure(measure: T.Measure, metrics: T.Dict<T.Metric>): T.MeasureEnhanced {
  return {
    ...measure,
    metric: metrics[measure.metric],
    leak: getLeakValue(measure)
  };
}

export function getLeakValue(measure: T.Measure | undefined): string | undefined {
  if (!measure || !measure.periods) {
    return undefined;
  }
  const period = getLeakPeriod(measure.periods);
  return period && period.value;
}

export function getRatingTooltip(metricKey: string, value: number): string | undefined {
  const finalMetricKey = isDiffMetric(metricKey) ? metricKey.substr(4) : metricKey;
  if (KNOWN_RATINGS.includes(finalMetricKey)) {
    return nextGetRatingTooltip(finalMetricKey, value);
  }
  return undefined;
}
