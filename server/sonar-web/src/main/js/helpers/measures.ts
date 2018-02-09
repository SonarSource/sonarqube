/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { translate, translateWithParameters, getCurrentLocale } from './l10n';
import { Metric } from '../app/types';

const HOURS_IN_DAY = 8;

export interface MeasurePeriod {
  index: number;
  value: string;
}

export interface MeasureIntern {
  value?: string;
  periods?: MeasurePeriod[];
}

export interface Measure extends MeasureIntern {
  metric: string;
}

export interface MeasureEnhanced extends MeasureIntern {
  metric: Metric;
  leak?: string;
}

interface Formatter {
  (value: string | number, options?: any): string;
}

/** Format a measure value for a given type */
export function formatMeasure(
  value: string | number | undefined,
  type: string,
  options?: any
): string {
  const formatter = getFormatter(type);
  return useFormatter(value, formatter, options);
}

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
  measures: Measure[],
  metrics: Metric[]
): MeasureEnhanced[] {
  return measures.map(measure => {
    const metric = metrics.find(metric => metric.key === measure.metric) as Metric;
    return { ...measure, metric };
  });
}

/** Get period value of a measure */
export function getPeriodValue(
  measure: Measure | MeasureEnhanced,
  periodIndex: number
): string | undefined {
  const { periods } = measure;
  const period = periods && periods.find(period => period.index === periodIndex);
  return period ? period.value : undefined;
}

/** Check if metric is differential */
export function isDiffMetric(metricKey: string): boolean {
  return metricKey.indexOf('new_') === 0;
}

function useFormatter(
  value: string | number | undefined,
  formatter: Formatter,
  options?: any
): string {
  return value !== undefined && value !== '' ? formatter(value, options) : '';
}

function getFormatter(type: string): Formatter {
  const FORMATTERS: { [type: string]: Formatter } = {
    INT: intFormatter,
    SHORT_INT: shortIntFormatter,
    FLOAT: floatFormatter,
    PERCENT: percentFormatter,
    WORK_DUR: durationFormatter,
    SHORT_WORK_DUR: shortDurationFormatter,
    RATING: ratingFormatter,
    LEVEL: levelFormatter,
    MILLISEC: millisecondsFormatter
  };
  return FORMATTERS[type] || noFormatter;
}

function numberFormatter(
  value: number,
  minimumFractionDigits = 0,
  maximumFractionDigits = minimumFractionDigits
) {
  const { format } = new Intl.NumberFormat(getCurrentLocale(), {
    minimumFractionDigits,
    maximumFractionDigits
  });
  return format(value);
}

function noFormatter(value: string | number): string | number {
  return value;
}

function intFormatter(value: number): string {
  return numberFormatter(value);
}

function shortIntFormatter(value: number): string {
  if (value >= 1e9) {
    return numberFormatter(value / 1e9) + translate('short_number_suffix.g');
  } else if (value >= 1e6) {
    return numberFormatter(value / 1e6) + translate('short_number_suffix.m');
  } else if (value >= 1e4) {
    return numberFormatter(value / 1e3) + translate('short_number_suffix.k');
  } else if (value >= 1e3) {
    return numberFormatter(value / 1e3, 0, 1) + translate('short_number_suffix.k');
  } else {
    return numberFormatter(value);
  }
}

function floatFormatter(value: number): string {
  return numberFormatter(value, 1, 5);
}

function percentFormatter(value: string | number, options: { decimals?: number } = {}): string {
  if (typeof value === 'string') {
    value = parseFloat(value);
  }
  if (options.decimals) {
    return numberFormatter(value, options.decimals) + '%';
  }
  return value === 100 ? '100%' : numberFormatter(value, 1) + '%';
}

function ratingFormatter(value: string | number): string {
  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }
  return String.fromCharCode(97 + value - 1).toUpperCase();
}

function levelFormatter(value: string): string {
  const l10nKey = 'metric.level.' + value;
  const result = translate(l10nKey);

  // if couldn't translate, return the initial value
  return l10nKey !== result ? result : value;
}

function millisecondsFormatter(value: number): string {
  const ONE_SECOND = 1000;
  const ONE_MINUTE = 60 * ONE_SECOND;
  if (value >= ONE_MINUTE) {
    const minutes = Math.round(value / ONE_MINUTE);
    return `${minutes}min`;
  } else if (value >= ONE_SECOND) {
    const seconds = Math.round(value / ONE_SECOND);
    return `${seconds}s`;
  } else {
    return `${value}ms`;
  }
}

/*
 * Debt Formatters
 */

function shouldDisplayDays(days: number): boolean {
  return days > 0;
}

function shouldDisplayDaysInShortFormat(days: number): boolean {
  return days > 0.9;
}

function shouldDisplayHours(days: number, hours: number): boolean {
  return hours > 0 && days < 10;
}

function shouldDisplayHoursInShortFormat(hours: number): boolean {
  return hours > 0.9;
}

function shouldDisplayMinutes(days: number, hours: number, minutes: number): boolean {
  return minutes > 0 && hours < 10 && days === 0;
}

function addSpaceIfNeeded(value: string): string {
  return value.length > 0 ? value + ' ' : value;
}

function formatDuration(isNegative: boolean, days: number, hours: number, minutes: number): string {
  let formatted = '';
  if (shouldDisplayDays(days)) {
    formatted += translateWithParameters('work_duration.x_days', isNegative ? -1 * days : days);
  }
  if (shouldDisplayHours(days, hours)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters(
      'work_duration.x_hours',
      isNegative && formatted.length === 0 ? -1 * hours : hours
    );
  }
  if (shouldDisplayMinutes(days, hours, minutes)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters(
      'work_duration.x_minutes',
      isNegative && formatted.length === 0 ? -1 * minutes : minutes
    );
  }
  return formatted;
}

function formatDurationShort(
  isNegative: boolean,
  days: number,
  hours: number,
  minutes: number
): string {
  if (shouldDisplayDaysInShortFormat(days)) {
    const roundedDays = Math.round(days);
    const formattedDays = formatMeasure(isNegative ? -1 * roundedDays : roundedDays, 'SHORT_INT');
    return translateWithParameters('work_duration.x_days', formattedDays);
  }

  if (shouldDisplayHoursInShortFormat(hours)) {
    const roundedHours = Math.round(hours);
    const formattedHours = formatMeasure(
      isNegative ? -1 * roundedHours : roundedHours,
      'SHORT_INT'
    );
    return translateWithParameters('work_duration.x_hours', formattedHours);
  }

  const formattedMinutes = formatMeasure(isNegative ? -1 * minutes : minutes, 'SHORT_INT');
  return translateWithParameters('work_duration.x_minutes', formattedMinutes);
}

function durationFormatter(value: string | number): string {
  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }
  if (value === 0) {
    return '0';
  }
  const hoursInDay = HOURS_IN_DAY;
  const isNegative = value < 0;
  const absValue = Math.abs(value);
  const days = Math.floor(absValue / hoursInDay / 60);
  let remainingValue = absValue - days * hoursInDay * 60;
  const hours = Math.floor(remainingValue / 60);
  remainingValue -= hours * 60;
  return formatDuration(isNegative, days, hours, remainingValue);
}

function shortDurationFormatter(value: string | number): string {
  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }
  if (value === 0) {
    return '0';
  }
  const hoursInDay = HOURS_IN_DAY;
  const isNegative = value < 0;
  const absValue = Math.abs(value);
  const days = absValue / hoursInDay / 60;
  let remainingValue = absValue - Math.floor(days) * hoursInDay * 60;
  const hours = remainingValue / 60;
  remainingValue -= Math.floor(hours) * 60;
  return formatDurationShort(isNegative, days, hours, remainingValue);
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
