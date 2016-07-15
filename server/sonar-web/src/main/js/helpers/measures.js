/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import numeral from 'numeral';
import _ from 'underscore';
import { translate, translateWithParameters } from './l10n';

/**
 * Format a measure value for a given type
 * @param {string|number} value
 * @param {string} type
 */
export function formatMeasure (value, type) {
  const formatter = getFormatter(type);
  return useFormatter(value, formatter);
}

/**
 * Format a measure variation for a given type
 * @param {string|number} value
 * @param {string} type
 */
export function formatMeasureVariation (value, type) {
  const formatter = getVariationFormatter(type);
  return useFormatter(value, formatter);
}

/**
 * Return a localized metric name
 * @param {string} metricKey
 * @returns {string}
 */
export function localizeMetric (metricKey) {
  return translate('metric', metricKey, 'name');
}

/**
 * Group list of metrics by their domain
 * @param {Array} metrics
 * @returns {Array}
 */
export function groupByDomain (metrics) {
  const groupedMetrics = _.groupBy(metrics, 'domain');
  const domains = _.map(groupedMetrics, (metricList, domain) => {
    return {
      domain,
      metrics: _.sortBy(metricList, 'name')
    };
  });
  return _.sortBy(domains, 'domain');
}

/**
 * Return corresponding "short" for better display in UI
 * @param {string} type
 * @returns {string}
 */
export function getShortType (type) {
  if (type === 'INT') {
    return 'SHORT_INT';
  } else if (type === 'WORK_DUR') {
    return 'SHORT_WORK_DUR';
  }
  return type;
}

/**
 * Map metrics
 * @param {Array} measures
 * @param {Array} metrics
 * @returns {Array}
 */
export function enhanceMeasuresWithMetrics (measures, metrics) {
  return measures.map(measure => {
    const metric = metrics.find(metric => metric.key === measure.metric);
    return { ...measure, metric };
  });
}

/**
 * Get period value of a measure
 * @param measure
 * @param periodIndex
 */
export function getPeriodValue (measure, periodIndex) {
  const { periods } = measure;
  const period = periods.find(period => period.index === periodIndex);
  return period ? period.value : null;
}

/**
 * Check if metric is differential
 * @param {string} metricKey
 * @returns {boolean}
 */
export function isDiffMetric (metricKey) {
  return metricKey.indexOf('new_') === 0;
}

/**
 * Check all types of coverage and return most suitable one
 * @param {Array} measures
 * @returns {string}
 */
export function selectCoverageMetric (measures) {
  const hasOverallCoverage = !!measures.find(measure => measure.metric === 'overall_coverage');
  const hasUTCoverage = !!measures.find(measure => measure.metric === 'coverage');
  const hasITCoverage = !!measures.find(measure => measure.metric === 'it_coverage');

  if (hasOverallCoverage && hasUTCoverage && hasITCoverage) {
    return 'overall_';
  } else if (hasITCoverage) {
    return 'it_';
  } else {
    return '';
  }
}

/*
 * Helpers
 */

function useFormatter (value, formatter) {
  return value != null && value !== '' && formatter != null ?
      formatter(value) : null;
}

function getFormatter (type) {
  const FORMATTERS = {
    'INT': intFormatter,
    'SHORT_INT': shortIntFormatter,
    'FLOAT': floatFormatter,
    'PERCENT': percentFormatter,
    'WORK_DUR': durationFormatter,
    'SHORT_WORK_DUR': shortDurationFormatter,
    'RATING': ratingFormatter,
    'LEVEL': levelFormatter,
    'MILLISEC': millisecondsFormatter
  };
  return FORMATTERS[type] || noFormatter;
}

function getVariationFormatter (type) {
  const FORMATTERS = {
    'INT': intVariationFormatter,
    'SHORT_INT': shortIntVariationFormatter,
    'FLOAT': floatVariationFormatter,
    'PERCENT': percentVariationFormatter,
    'WORK_DUR': durationVariationFormatter,
    'SHORT_WORK_DUR': shortDurationVariationFormatter,
    'RATING': emptyFormatter,
    'LEVEL': emptyFormatter,
    'MILLISEC': millisecondsVariationFormatter
  };
  return FORMATTERS[type] || noFormatter;
}

/*
 * Formatters
 */

function noFormatter (value) {
  return value;
}

function emptyFormatter () {
  return null;
}

function intFormatter (value) {
  return numeral(value).format('0,0');
}

function intVariationFormatter (value) {
  return numeral(value).format('+0,0');
}

function shortIntFormatter (value) {
  let format = '0,0';
  if (value >= 1000) {
    format = '0.[0]a';
  }
  if (value >= 10000) {
    format = '0a';
  }
  return numeral(value).format(format);
}

function shortIntVariationFormatter (value) {
  const formatted = shortIntFormatter(Math.abs(value));
  return value < 0 ? `-${formatted}` : `+${formatted}`;
}

function floatFormatter (value) {
  return numeral(value).format('0,0.0[0000]');
}

function floatVariationFormatter (value) {
  return value === 0 ? '+0.0' : numeral(value).format('+0,0.0[0000]');
}

function percentFormatter (value) {
  value = parseFloat(value);
  return numeral(value / 100).format('0,0.0%');
}

function percentVariationFormatter (value) {
  value = parseFloat(value);
  return value === 0 ? '+0.0%' : numeral(value / 100).format('+0,0.0%');
}

function ratingFormatter (value) {
  value = parseInt(value, 10);
  return String.fromCharCode(97 + value - 1).toUpperCase();
}

function levelFormatter (value) {
  const l10nKey = 'metric.level.' + value;
  const result = translate(l10nKey);

  // if couldn't translate, return the initial value
  return l10nKey !== result ? result : value;
}

function millisecondsFormatter (value) {
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

function millisecondsVariationFormatter (value) {
  const absValue = Math.abs(value);
  const formattedValue = millisecondsFormatter(absValue);
  return value < 0 ? `-${formattedValue}` : `+${formattedValue}`;
}

/*
 * Debt Formatters
 */

function shouldDisplayDays (days) {
  return days > 0;
}

function shouldDisplayHours (days, hours) {
  return hours > 0 && days < 10;
}

function shouldDisplayHoursInShortFormat (days, hours) {
  return hours > 0 && days === 0;
}

function shouldDisplayMinutes (days, hours, minutes) {
  return minutes > 0 && hours < 10 && days === 0;
}

function shouldDisplayMinutesInShortFormat (days, hours, minutes) {
  return minutes > 0 && hours === 0 && days === 0;
}

function addSpaceIfNeeded (value) {
  return value.length > 0 ? value + ' ' : value;
}

function formatDuration (isNegative, days, hours, minutes) {
  let formatted = '';
  if (shouldDisplayDays(days)) {
    formatted += translateWithParameters('work_duration.x_days', isNegative ? -1 * days : days);
  }
  if (shouldDisplayHours(days, hours)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters('work_duration.x_hours',
        isNegative && formatted.length === 0 ? -1 * hours : hours);
  }
  if (shouldDisplayMinutes(days, hours, minutes)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters('work_duration.x_minutes',
        isNegative && formatted.length === 0 ? -1 * minutes : minutes);
  }
  return formatted;
}

function formatDurationShort (isNegative, days, hours, minutes) {
  let formatted = '';
  if (shouldDisplayDays(days)) {
    const formattedDays = formatMeasure(isNegative ? -1 * days : days, 'SHORT_INT');
    formatted += translateWithParameters('work_duration.x_days', formattedDays);
  }
  if (shouldDisplayHoursInShortFormat(days, hours)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters('work_duration.x_hours',
        isNegative && formatted.length === 0 ? -1 * hours : hours);
  }
  if (shouldDisplayMinutesInShortFormat(days, hours, minutes)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += translateWithParameters('work_duration.x_minutes',
        isNegative && formatted.length === 0 ? -1 * minutes : minutes);
  }
  return formatted;
}

function durationFormatter (value) {
  if (value === 0 || value === '0') {
    return '0';
  }
  const hoursInDay = window.SS.hoursInDay;
  const isNegative = value < 0;
  const absValue = Math.abs(value);
  const days = Math.floor(absValue / hoursInDay / 60);
  let remainingValue = absValue - days * hoursInDay * 60;
  const hours = Math.floor(remainingValue / 60);
  remainingValue -= hours * 60;
  return formatDuration(isNegative, days, hours, remainingValue);
}

function shortDurationFormatter (value) {
  value = parseInt(value, 10);
  if (value === 0 || value === '0') {
    return '0';
  }
  const hoursInDay = window.SS.hoursInDay;
  const isNegative = value < 0;
  const absValue = Math.abs(value);
  const days = Math.floor(absValue / hoursInDay / 60);
  let remainingValue = absValue - days * hoursInDay * 60;
  const hours = Math.floor(remainingValue / 60);
  remainingValue -= hours * 60;
  return formatDurationShort(isNegative, days, hours, remainingValue);
}

function durationVariationFormatter (value) {
  if (value === 0 || value === '0') {
    return '+0';
  }
  const formatted = durationFormatter(value);
  return formatted[0] !== '-' ? '+' + formatted : formatted;
}

function shortDurationVariationFormatter (value) {
  if (value === 0 || value === '0') {
    return '+0';
  }
  const formatted = shortDurationFormatter(value);
  return formatted[0] !== '-' ? '+' + formatted : formatted;
}


let maintainabilityRatingGrid;
export function getMaintainabilityRatingGrid () {
  if (maintainabilityRatingGrid) {
    return maintainabilityRatingGrid;
  }

  const str = window.SS['sonar.technicalDebt.ratingGrid'];
  const numbers = str.split(',')
      .map(s => parseFloat(s))
      .filter(n => !isNaN(n));

  if (numbers.length === 4) {
    maintainabilityRatingGrid = numbers;
  } else {
    maintainabilityRatingGrid = [0, 0, 0, 0];
  }

  return maintainabilityRatingGrid;
}
