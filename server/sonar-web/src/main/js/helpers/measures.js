import numeral from 'numeral';
import _ from 'underscore';


/**
 * Format a measure value for a given type
 * @param {string|number} value
 * @param {string} type
 */
export function formatMeasure (value, type) {
  let formatter = getFormatter(type);
  return useFormatter(value, formatter);
}


/**
 * Format a measure variation for a given type
 * @param {string|number} value
 * @param {string} type
 */
export function formatMeasureVariation (value, type) {
  let formatter = getVariationFormatter(type);
  return useFormatter(value, formatter);
}


/**
 * Return a localized metric name
 * @param {string} metricKey
 * @returns {string}
 */
export function localizeMetric (metricKey) {
  return window.t('metric', metricKey, 'name');
}


/**
 * Group list of metrics by their domain
 * @param {Array} metrics
 * @returns {Array}
 */
export function groupByDomain (metrics) {
  let groupedMetrics = _.groupBy(metrics, 'domain');
  let domains = _.map(groupedMetrics, (metricList, domain) => {
    return {
      domain: domain,
      metrics: _.sortBy(metricList, 'name')
    };
  });
  return _.sortBy(domains, 'domain');
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
    'RATING': ratingFormatter,
    'LEVEL': levelFormatter,
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

function intFormatter (value) {
  return numeral(value).format('0,0');
}

function intVariationFormatter (value) {
  return numeral(value).format('+0,0');
}

function shortIntFormatter (value) {
  var format = '0,0';
  if (value >= 1000) {
    format = '0.[0]a';
  }
  if (value >= 10000) {
    format = '0a';
  }
  return numeral(value).format(format);
}

function shortIntVariationFormatter (value) {
  let formatted = shortIntFormatter(Math.abs(value));
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
  var l10nKey = 'metric.level.' + value,
      result = window.t(l10nKey);
  // if couldn't translate, return the initial value
  return l10nKey !== result ? result : value;
}

function millisecondsFormatter (value) {
  const ONE_SECOND = 1000;
  const ONE_MINUTE = 60 * ONE_SECOND;
  if (value >= ONE_MINUTE) {
    let minutes = Math.round(value / ONE_MINUTE);
    return `${minutes}min`;
  } else if (value >= ONE_SECOND) {
    let seconds = Math.round(value / ONE_SECOND);
    return `${seconds}s`;
  } else {
    return `${value}ms`;
  }
}

function millisecondsVariationFormatter (value) {
  let absValue = Math.abs(value);
  let formattedValue = millisecondsFormatter(absValue);
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
  var formatted = '';
  if (shouldDisplayDays(days)) {
    formatted += window.tp('work_duration.x_days', isNegative ? -1 * days : days);
  }
  if (shouldDisplayHours(days, hours)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += window.tp('work_duration.x_hours', isNegative && formatted.length === 0 ? -1 * hours : hours);
  }
  if (shouldDisplayMinutes(days, hours, minutes)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += window.tp('work_duration.x_minutes', isNegative && formatted.length === 0 ? -1 * minutes : minutes);
  }
  return formatted;
}

function formatDurationShort (isNegative, days, hours, minutes) {
  var formatted = '';
  if (shouldDisplayDays(days)) {
    var formattedDays = formatMeasure(isNegative ? -1 * days : days, 'SHORT_INT');
    formatted += window.tp('work_duration.x_days', formattedDays);
  }
  if (shouldDisplayHoursInShortFormat(days, hours)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += window.tp('work_duration.x_hours', isNegative && formatted.length === 0 ? -1 * hours : hours);
  }
  if (shouldDisplayMinutesInShortFormat(days, hours, minutes)) {
    formatted = addSpaceIfNeeded(formatted);
    formatted += window.tp('work_duration.x_minutes', isNegative && formatted.length === 0 ? -1 * minutes : minutes);
  }
  return formatted;
}

function durationFormatter (value) {
  if (value === 0) {
    return '0';
  }
  var hoursInDay = window.SS.hoursInDay,
      isNegative = value < 0,
      absValue = Math.abs(value);
  var days = Math.floor(absValue / hoursInDay / 60);
  var remainingValue = absValue - days * hoursInDay * 60;
  var hours = Math.floor(remainingValue / 60);
  remainingValue -= hours * 60;
  return formatDuration(isNegative, days, hours, remainingValue);
}

function shortDurationFormatter (value) {
  value = parseInt(value, 10);
  if (value === 0) {
    return '0';
  }
  var hoursInDay = window.SS.hoursInDay,
      isNegative = value < 0,
      absValue = Math.abs(value);
  var days = Math.floor(absValue / hoursInDay / 60);
  var remainingValue = absValue - days * hoursInDay * 60;
  var hours = Math.floor(remainingValue / 60);
  remainingValue -= hours * 60;
  return formatDurationShort(isNegative, days, hours, remainingValue);
}

function durationVariationFormatter (value) {
  if (value === 0) {
    return '0';
  }
  var formatted = durationFormatter(value);
  return formatted[0] !== '-' ? '+' + formatted : formatted;
}

function shortDurationVariationFormatter (value) {
  if (value === 0) {
    return '+0';
  }
  var formatted = shortDurationFormatter(value);
  return formatted[0] !== '-' ? '+' + formatted : formatted;
}
