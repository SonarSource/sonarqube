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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { isDefined } from 'sonar-ui-common/helpers/types';

/** Return a localized metric name */
export function localizeMetric(metricKey: string): string {
  return translate('metric', metricKey, 'name');
}

/** Return corresponding "short" for better display in UI */
export function getShortType(type: string): string {
  if (type === 'INT') {
    return 'SHORT_INT';
  } else if (type === 'WORK_DUR') {
    return 'SHORT_WORK_DUR';
  }
  return type;
}

export function enhanceMeasuresWithMetrics(
  measures: T.Measure[],
  metrics: T.Metric[]
): T.MeasureEnhanced[] {
  return measures
    .map(measure => {
      const metric = metrics.find(metric => metric.key === measure.metric);
      return metric && { ...measure, metric };
    })
    .filter(isDefined);
}

/** Get period value of a measure */
export function getPeriodValue(
  measure: T.Measure | T.MeasureEnhanced,
  periodIndex: number
): string | undefined {
  const { periods } = measure;
  const period = periods && periods.find(period => period.index === periodIndex);
  return period ? period.value : undefined;
}

export function isPeriodBestValue(
  measure: T.Measure | T.MeasureEnhanced,
  periodIndex: number
): boolean {
  const { periods } = measure;
  const period = periods && periods.find(period => period.index === periodIndex);
  return (period && period.bestValue) || false;
}

/** Check if metric is differential */
export function isDiffMetric(metricKey: string): boolean {
  return metricKey.indexOf('new_') === 0;
}

function getRatingGrid(): string {
  // workaround cyclic dependencies
  const getStore = require('../app/utils/getStore').default;
  const { getGlobalSettingValue } = require('../store/rootReducer');

  const store = getStore();
  const settingValue = getGlobalSettingValue(store.getState(), 'sonar.technicalDebt.ratingGrid');
  return settingValue ? settingValue.value : '';
}

let maintainabilityRatingGrid: number[];

function getMaintainabilityRatingGrid(): number[] {
  if (maintainabilityRatingGrid) {
    return maintainabilityRatingGrid;
  }

  const str = getRatingGrid();
  const numbers = str
    .split(',')
    .map(s => parseFloat(s))
    .filter(n => !isNaN(n));

  if (numbers.length === 4) {
    maintainabilityRatingGrid = numbers;
  } else {
    maintainabilityRatingGrid = [0, 0, 0, 0];
  }

  return maintainabilityRatingGrid;
}

function getMaintainabilityRatingTooltip(rating: number): string {
  const maintainabilityGrid = getMaintainabilityRatingGrid();
  const maintainabilityRatingThreshold = maintainabilityGrid[Math.floor(rating) - 2];

  if (rating < 2) {
    return translateWithParameters(
      'metric.sqale_rating.tooltip.A',
      formatMeasure(maintainabilityGrid[0] * 100, 'PERCENT')
    );
  }

  const ratingLetter = formatMeasure(rating, 'RATING');

  return translateWithParameters(
    'metric.sqale_rating.tooltip',
    ratingLetter,
    formatMeasure(maintainabilityRatingThreshold * 100, 'PERCENT')
  );
}

export function getRatingTooltip(metricKey: string, value: number | string): string {
  const ratingLetter = formatMeasure(value, 'RATING');

  const finalMetricKey = isDiffMetric(metricKey) ? metricKey.substr(4) : metricKey;

  return finalMetricKey === 'sqale_rating' || finalMetricKey === 'maintainability_rating'
    ? getMaintainabilityRatingTooltip(Number(value))
    : translate('metric', finalMetricKey, 'tooltip', ratingLetter);
}

export function getDisplayMetrics(metrics: T.Metric[]) {
  return metrics.filter(metric => !metric.hidden && !['DATA', 'DISTRIB'].includes(metric.type));
}

export function findMeasure(measures: T.Measure[], metric: string) {
  return measures.find(measure => measure.metric === metric);
}
