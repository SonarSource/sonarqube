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
import bubbles from './bubbles';
import { formatMeasure, formatMeasureVariation } from '../../helpers/measures';

export function getLeakValue (measure) {
  if (!measure) {
    return null;
  }

  const period = measure.periods ?
      measure.periods.find(period => period.index === 1) : null;

  return period ? period.value : null;
}

export function getSingleMeasureValue (measures) {
  if (!measures || !measures.length) {
    return null;
  }

  return measures[0].value;
}

export function getSingleLeakValue (measures) {
  if (!measures || !measures.length) {
    return null;
  }

  const measure = measures[0];

  const period = measure.periods ?
      measure.periods.find(period => period.index === 1) : null;

  return period ? period.value : null;
}

export function formatLeak (value, metric) {
  if (metric.key.indexOf('new_') === 0) {
    return formatMeasure(value, metric.type);
  } else {
    return formatMeasureVariation(value, metric.type);
  }
}

export function enhanceWithLeak (measures) {
  function enhanceSingle (measure) {
    return { ...measure, leak: getLeakValue(measure) };
  }

  if (Array.isArray(measures)) {
    return measures.map(enhanceSingle);
  } else {
    return enhanceSingle(measures);
  }
}

export function enhanceWithSingleMeasure (components) {
  return components
      .map(component => {
        return {
          ...component,
          value: getSingleMeasureValue(component.measures),
          leak: getSingleLeakValue(component.measures)
        };
      })
      .filter(component => component.value != null || component.leak != null);
}

export function hasHistory (metricKey) {
  return metricKey.indexOf('new_') !== 0;
}

export function hasBubbleChart (metricKey) {
  return !!bubbles[metricKey];
}

export function hasTreemap (metric) {
  return ['PERCENT', 'RATING', 'LEVEL'].indexOf(metric.type) !== -1;
}
