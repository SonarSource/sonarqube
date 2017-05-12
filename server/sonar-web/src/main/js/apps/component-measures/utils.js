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
import bubbles from './config/bubbles';
import {
  formatMeasure,
  formatMeasureVariation,
  getRatingTooltip as nextGetRatingTooltip
} from '../../helpers/measures';

export function isDiffMetric(metric) {
  return metric.key.indexOf('new_') === 0;
}

export function getLeakValue(measure, periodIndex = 1) {
  if (!measure) {
    return null;
  }

  const period = measure.periods
    ? measure.periods.find(period => period.index === periodIndex)
    : null;

  return period ? period.value : null;
}

export function getSingleMeasureValue(measures) {
  if (!measures || !measures.length) {
    return null;
  }

  return measures[0].value;
}

export function getSingleLeakValue(measures, periodIndex = 1) {
  if (!measures || !measures.length) {
    return null;
  }

  const measure = measures[0];

  const period = measure.periods
    ? measure.periods.find(period => period.index === periodIndex)
    : null;

  return period ? period.value : null;
}

export function formatLeak(value, metric, options) {
  if (isDiffMetric(metric)) {
    return formatMeasure(value, metric.type, options);
  } else {
    return formatMeasureVariation(value, metric.type, options);
  }
}

export function enhanceWithLeak(measures, periodIndex = 1) {
  function enhanceSingle(measure) {
    return { ...measure, leak: getLeakValue(measure, periodIndex) };
  }

  if (Array.isArray(measures)) {
    return measures.map(enhanceSingle);
  } else {
    return enhanceSingle(measures);
  }
}

export function enhanceWithSingleMeasure(components, periodIndex = 1) {
  return components.map(component => {
    return {
      ...component,
      value: getSingleMeasureValue(component.measures),
      leak: getSingleLeakValue(component.measures, periodIndex)
    };
  });
}

export function enhanceWithMeasure(components, metric, periodIndex = 1) {
  return components.map(component => {
    const measuresWithLeak = enhanceWithLeak(component.measures, periodIndex);
    const measure = measuresWithLeak.find(measure => measure.metric === metric);
    const value = measure ? measure.value : null;
    const leak = measure ? measure.leak : null;
    return { ...component, value, leak, measures: measuresWithLeak };
  });
}

export function hasHistory(metricKey) {
  return metricKey.indexOf('new_') !== 0;
}

export function hasBubbleChart(domainName) {
  return !!bubbles[domainName];
}

export function hasTreemap(metric) {
  return ['PERCENT', 'RATING', 'LEVEL'].indexOf(metric.type) !== -1;
}

export function filterOutEmptyMeasures(components) {
  return components.filter(component => component.value !== null || component.leak !== null);
}

export function getRatingTooltip(metricKey, value) {
  const finalMetricKey = metricKey.indexOf('new_') === 0 ? metricKey.substr(4) : metricKey;
  const KNOWN_RATINGS = ['sqale_rating', 'reliability_rating', 'security_rating'];
  if (KNOWN_RATINGS.includes(finalMetricKey)) {
    return nextGetRatingTooltip(finalMetricKey, value);
  }
  return null;
}
