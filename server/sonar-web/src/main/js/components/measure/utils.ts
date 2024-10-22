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

import { MetricKey } from '../../sonar-aligned/types/metrics';
import { Dict, Measure, MeasureEnhanced, MeasureIntern, Metric } from '../../types/types';

export const KNOWN_RATINGS = [
  MetricKey.sqale_rating,
  MetricKey.reliability_rating,
  MetricKey.security_rating,
  MetricKey.security_review_rating,
  MetricKey.software_quality_maintainability_rating,
  MetricKey.software_quality_reliability_rating,
  MetricKey.software_quality_security_rating,
  'maintainability_rating', // Needed to provide the label for "new_maintainability_rating"
];

export function enhanceMeasure(measure: Measure, metrics: Dict<Metric>): MeasureEnhanced {
  return {
    ...measure,
    metric: metrics[measure.metric],
    leak: getLeakValue(measure),
  };
}

export function getLeakValue(measure: MeasureIntern | undefined): string | undefined {
  return measure?.period?.value;
}

export function duplicationRatingConverter(val: number) {
  const value = val || 0;
  const THRESHOLD_A = 3;
  const THRESHOLD_B = 5;
  const THRESHOLD_C = 10;
  const THRESHOLD_D = 20;

  if (value < THRESHOLD_A) {
    return 'A';
  } else if (value < THRESHOLD_B) {
    return 'B';
  } else if (value < THRESHOLD_C) {
    return 'C';
  } else if (value < THRESHOLD_D) {
    return 'D';
  }
  return 'E';
}
