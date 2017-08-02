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
// @flow
import { groupBy, memoize, sortBy, toPairs } from 'lodash';
import { getLocalizedMetricName } from '../../helpers/l10n';
import { cleanQuery, parseAsString, serializeString } from '../../helpers/query';
import { domains } from './config/domains';
import { bubbles } from './config/bubbles';
import { enhanceMeasure } from '../../components/measure/utils';
import type { Component, ComponentEnhanced, Query } from './types';
import type { RawQuery } from '../../helpers/query';
import type { Metric } from '../../store/metrics/actions';
import type { MeasureEnhanced } from '../../components/measure/types';

export const DEFAULT_VIEW = 'list';
const KNOWN_DOMAINS = [
  'Releasability',
  'Reliability',
  'Security',
  'Maintainability',
  'Coverage',
  'Duplications',
  'Size',
  'Complexity'
];
const BANNED_MEASURES = [
  'blocker_violations',
  'new_blocker_violations',
  'critical_violations',
  'new_critical_violations',
  'major_violations',
  'new_major_violations',
  'minor_violations',
  'new_minor_violations',
  'info_violations',
  'new_info_violations'
];

export function filterMeasures(measures: Array<MeasureEnhanced>): Array<MeasureEnhanced> {
  return measures.filter(measure => !BANNED_MEASURES.includes(measure.metric.key));
}

export function sortMeasures(
  domainName: string,
  measures: Array<MeasureEnhanced>
): Array<MeasureEnhanced> {
  const config = domains[domainName] || {};
  const configOrder = config.order || [];
  return sortBy(measures, [
    measure => {
      const idx = configOrder.indexOf(measure.metric.key);
      return idx >= 0 ? idx : configOrder.length;
    },
    measure => getLocalizedMetricName(measure.metric)
  ]);
}

export const enhanceComponent = (component: Component, metric: Metric): ComponentEnhanced => {
  const enhancedMeasures = component.measures.map(measure => enhanceMeasure(measure, metric));
  const measure = enhancedMeasures.find(measure => measure.metric.key === metric.key);
  const value = measure ? measure.value : null;
  const leak = measure ? measure.leak : null;
  return { ...component, value, leak, measures: enhancedMeasures };
};

export const isFileType = (component: Component): boolean =>
  ['FIL', 'UTS'].includes(component.qualifier);

export const groupByDomains = memoize((measures: Array<MeasureEnhanced>): Array<{
  name: string,
  measures: Array<MeasureEnhanced>
}> => {
  const domains = toPairs(groupBy(measures, measure => measure.metric.domain)).map(r => {
    const [name, measures] = r;
    const sortedMeasures = sortBy(measures, measure => getLocalizedMetricName(measure.metric));
    return { name, measures: sortedMeasures };
  });

  return sortBy(domains, [
    domain => {
      const idx = KNOWN_DOMAINS.indexOf(domain.name);
      return idx >= 0 ? idx : KNOWN_DOMAINS.length;
    },
    'name'
  ]);
});

export const hasTreemap = (metricType: string): boolean =>
  ['PERCENT', 'RATING', 'LEVEL'].includes(metricType);

export const hasBubbleChart = (domainName: string): boolean => bubbles[domainName] != null;

export const parseQuery = memoize((urlQuery: RawQuery): Query => ({
  metric: parseAsString(urlQuery['metric']),
  selected: parseAsString(urlQuery['selected']),
  view: parseAsString(urlQuery['view']) || DEFAULT_VIEW
}));

export const serializeQuery = memoize((query: Query): RawQuery => {
  return cleanQuery({
    metric: serializeString(query.metric),
    selected: serializeString(query.selected),
    view: query.view === DEFAULT_VIEW ? null : serializeString(query.view)
  });
});
