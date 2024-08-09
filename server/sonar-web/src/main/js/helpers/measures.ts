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
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import {
  QualityGateStatusCondition,
  QualityGateStatusConditionEnhanced,
} from '../types/quality-gates';
import { Measure, MeasureEnhanced, Metric } from '../types/types';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  LEAK_CCT_SOFTWARE_QUALITY_METRICS,
  LEAK_OLD_TAXONOMY_METRICS,
  SOFTWARE_QUALITY_RATING_METRICS,
} from './constants';
import { translate } from './l10n';
import { isDefined } from './types';

export const MEASURES_REDIRECTION: Partial<Record<MetricKey, MetricKey>> = {
  [MetricKey.wont_fix_issues]: MetricKey.accepted_issues,
  [MetricKey.open_issues]: MetricKey.violations,
  [MetricKey.reopened_issues]: MetricKey.violations,
};

export function enhanceMeasuresWithMetrics(
  measures: Measure[],
  metrics: Metric[],
): MeasureEnhanced[] {
  return measures
    .map((measure) => {
      const metric = metrics.find((metric) => metric.key === measure.metric);
      return metric && { ...measure, metric };
    })
    .filter(isDefined);
}

export function enhanceConditionWithMeasure(
  condition: QualityGateStatusCondition,
  measures: MeasureEnhanced[],
): QualityGateStatusConditionEnhanced | undefined {
  const measure = measures.find((m) => m.metric.key === condition.metric);

  // Make sure we have a period index. This is necessary when dealing with
  // applications.
  let { period } = condition;
  if (measure?.period && !period) {
    period = measure.period.index;
  }

  return measure && { ...condition, period, measure };
}

export function isPeriodBestValue(measure: Measure | MeasureEnhanced): boolean {
  return measure.period?.bestValue ?? false;
}

/** Check if metric is differential */
export function isDiffMetric(metricKey: MetricKey | string): boolean {
  return metricKey.startsWith('new_');
}

export function getDisplayMetrics(metrics: Metric[]) {
  return metrics.filter(
    (metric) =>
      !metric.hidden &&
      ([...CCT_SOFTWARE_QUALITY_METRICS, ...LEAK_CCT_SOFTWARE_QUALITY_METRICS].includes(
        metric.key as MetricKey,
      ) ||
        ![MetricType.Data, MetricType.Distribution].includes(metric.type as MetricType)),
  );
}

export function findMeasure(measures: MeasureEnhanced[], metric: MetricKey | string) {
  return measures.find((measure) => measure.metric.key === metric);
}

export function areLeakCCTMeasuresComputed(measures?: Measure[] | MeasureEnhanced[]) {
  if (
    LEAK_OLD_TAXONOMY_METRICS.every(
      (metric) =>
        !measures?.find((measure) =>
          isMeasureEnhanced(measure) ? measure.metric.key === metric : measure.metric === metric,
        ),
    )
  ) {
    return true;
  }
  return LEAK_CCT_SOFTWARE_QUALITY_METRICS.every((metric) =>
    measures?.find((measure) =>
      isMeasureEnhanced(measure) ? measure.metric.key === metric : measure.metric === metric,
    ),
  );
}

export function areCCTMeasuresComputed(measures?: Measure[] | MeasureEnhanced[]) {
  return CCT_SOFTWARE_QUALITY_METRICS.every((metric) =>
    measures?.find((measure) =>
      isMeasureEnhanced(measure) ? measure.metric.key === metric : measure.metric === metric,
    ),
  );
}
export function areSoftwareQualityRatingsComputed(measures?: Measure[] | MeasureEnhanced[]) {
  return SOFTWARE_QUALITY_RATING_METRICS.every((metric) =>
    measures?.find((measure) =>
      isMeasureEnhanced(measure) ? measure.metric.key === metric : measure.metric === metric,
    ),
  );
}

export function areLeakAndOverallCCTMeasuresComputed(measures?: Measure[] | MeasureEnhanced[]) {
  return areLeakCCTMeasuresComputed(measures) && areCCTMeasuresComputed(measures);
}

function isMeasureEnhanced(measure: Measure | MeasureEnhanced): measure is MeasureEnhanced {
  return (measure.metric as Metric)?.key !== undefined;
}

export const getCCTMeasureValue = (key: string, value?: string) => {
  if (
    CCT_SOFTWARE_QUALITY_METRICS.concat(LEAK_CCT_SOFTWARE_QUALITY_METRICS).includes(
      key as MetricKey,
    ) &&
    value !== undefined
  ) {
    return JSON.parse(value).total;
  }
  return value;
};

type RatingValue = 'A' | 'B' | 'C' | 'D' | 'E';
const RATING_VALUES: RatingValue[] = ['A', 'B', 'C', 'D', 'E'];
export function formatRating(value: string | number | undefined): RatingValue | undefined {
  if (!value) {
    return undefined;
  }

  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }

  // rating is 1-5, adjust for 0-based indexing
  return RATING_VALUES[value - 1];
}

/** Return a localized metric name */
export function localizeMetric(metricKey: string): string {
  return translate('metric', metricKey, 'name');
}

/** Return corresponding "short" for better display in UI */
export function getShortType(type: string): string {
  if (type === MetricType.Integer) {
    return MetricType.ShortInteger;
  } else if (type === 'WORK_DUR') {
    return MetricType.ShortWorkDuration;
  }
  return type;
}
