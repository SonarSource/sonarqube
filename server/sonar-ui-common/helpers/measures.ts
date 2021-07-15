/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { getCurrentLocale, translate, translateWithParameters } from './l10n';

const HOURS_IN_DAY = 8;

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
  // eslint-disable-next-line react-hooks/rules-of-hooks
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

/** Check if metric is differential */
export function isDiffMetric(metricKey: string): boolean {
  return metricKey.indexOf('new_') === 0;
}

/*
 * Conditional decimal count for QualityGate-impacting measures
 * (e.g. Coverage %)
 * Increase the number of decimals if the value is close to the threshold
 * We count the precision (number of 0's, i.e. log10 and round down) needed to show the difference
 * E.g. threshold 85, value 84.9993 -> delta = 0.0007, we need 4 decimals to see the difference
 * otherwise rounding will make it look like they are equal.
 */
const DEFAULT_DECIMALS = 1;
export function getMinDecimalsCountToBeDistinctFromThreshold(
  value: number,
  threshold: number | undefined
): number {
  if (!threshold) {
    return DEFAULT_DECIMALS;
  }
  const delta = Math.abs(threshold - value);
  if (delta < 0.1 && delta > 0) {
    return -Math.floor(Math.log10(delta));
  } else {
    return DEFAULT_DECIMALS;
  }
}

function useFormatter(
  value: string | number | undefined,
  formatter: Formatter,
  options?: any
): string {
  return value !== undefined && value !== '' ? formatter(value, options) : '';
}

function getFormatter(type: string): Formatter {
  const FORMATTERS: T.Dict<Formatter> = {
    INT: intFormatter,
    SHORT_INT: shortIntFormatter,
    FLOAT: floatFormatter,
    PERCENT: percentFormatter,
    WORK_DUR: durationFormatter,
    SHORT_WORK_DUR: shortDurationFormatter,
    RATING: ratingFormatter,
    LEVEL: levelFormatter,
    MILLISEC: millisecondsFormatter,
  };
  return FORMATTERS[type] || noFormatter;
}

function numberFormatter(
  value: string | number,
  minimumFractionDigits = 0,
  maximumFractionDigits = minimumFractionDigits
) {
  const { format } = new Intl.NumberFormat(getCurrentLocale(), {
    minimumFractionDigits,
    maximumFractionDigits,
  });
  if (typeof value === 'string') {
    return format(parseFloat(value));
  }
  return format(value);
}

function noFormatter(value: string | number): string | number {
  return value;
}

function intFormatter(value: string | number): string {
  return numberFormatter(value);
}

const shortIntFormats = [
  { unit: 1e10, formatUnit: 1e9, fraction: 0, suffix: 'short_number_suffix.g' },
  { unit: 1e9, formatUnit: 1e9, fraction: 1, suffix: 'short_number_suffix.g' },
  { unit: 1e7, formatUnit: 1e6, fraction: 0, suffix: 'short_number_suffix.m' },
  { unit: 1e6, formatUnit: 1e6, fraction: 1, suffix: 'short_number_suffix.m' },
  { unit: 1e4, formatUnit: 1e3, fraction: 0, suffix: 'short_number_suffix.k' },
  { unit: 1e3, formatUnit: 1e3, fraction: 1, suffix: 'short_number_suffix.k' },
];

function shortIntFormatter(
  value: string | number,
  option?: { roundingFunc?: (x: number) => number }
): string {
  const roundingFunc = (option && option.roundingFunc) || undefined;
  if (typeof value === 'string') {
    value = parseFloat(value);
  }
  for (let i = 0; i < shortIntFormats.length; i++) {
    const { unit, formatUnit, fraction, suffix } = shortIntFormats[i];
    const nextFraction = unit / (shortIntFormats[i + 1] ? shortIntFormats[i + 1].unit / 10 : 1);
    const roundedValue = numberRound(value / unit, nextFraction, roundingFunc);
    if (roundedValue >= 1) {
      return (
        numberFormatter(
          numberRound(value / formatUnit, Math.pow(10, fraction), roundingFunc),
          0,
          fraction
        ) + translate(suffix)
      );
    }
  }

  return numberFormatter(value);
}

function numberRound(
  value: number,
  fraction: number = 1000,
  roundingFunc: (x: number) => number = Math.round
) {
  return roundingFunc(value * fraction) / fraction;
}

function floatFormatter(value: string | number): string {
  return numberFormatter(value, 1, 5);
}

function percentFormatter(
  value: string | number,
  { decimals, omitExtraDecimalZeros }: { decimals?: number; omitExtraDecimalZeros?: boolean } = {}
): string {
  if (typeof value === 'string') {
    value = parseFloat(value);
  }
  if (value === 100) {
    return '100%';
  } else if (omitExtraDecimalZeros && decimals) {
    // If omitExtraDecimalZeros is true, all trailing decimal 0s will be removed,
    // except for the first decimal.
    // E.g. for decimals=3:
    // - omitExtraDecimalZeros: false, value: 45.450 => 45.450
    // - omitExtraDecimalZeros: true, value: 45.450 => 45.45
    // - omitExtraDecimalZeros: false, value: 85 => 85.000
    // - omitExtraDecimalZeros: true, value: 85 => 85.0
    return `${numberFormatter(value, 1, decimals)}%`;
  } else {
    return `${numberFormatter(value, decimals || 1)}%`;
  }
}

function ratingFormatter(value: string | number): string {
  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }
  return String.fromCharCode(97 + value - 1).toUpperCase();
}

function levelFormatter(value: string | number): string {
  if (typeof value === 'number') {
    value = value.toString();
  }
  const l10nKey = `metric.level.${value}`;
  const result = translate(l10nKey);

  // if couldn't translate, return the initial value
  return l10nKey !== result ? result : value;
}

function millisecondsFormatter(value: string | number): string {
  if (typeof value === 'string') {
    value = parseInt(value, 10);
  }
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
  return value.length > 0 ? `${value} ` : value;
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
