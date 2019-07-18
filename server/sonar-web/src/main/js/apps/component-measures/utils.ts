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
import { groupBy, memoize, sortBy, toPairs } from 'lodash';
import { getLocalizedMetricName } from 'sonar-ui-common/helpers/l10n';
import { cleanQuery, parseAsString, serializeString } from 'sonar-ui-common/helpers/query';
import { enhanceMeasure } from '../../components/measure/utils';
import {
  isLongLivingBranch,
  isMainBranch,
  isPullRequest,
  isShortLivingBranch
} from '../../helpers/branches';
import { getDisplayMetrics, isDiffMetric } from '../../helpers/measures';
import { bubbles } from './config/bubbles';
import { domains } from './config/domains';

export type View = 'list' | 'tree' | 'treemap';

export const PROJECT_OVERVEW = 'project_overview';
export const DEFAULT_VIEW: View = 'tree';
export const DEFAULT_METRIC = PROJECT_OVERVEW;
export const KNOWN_DOMAINS = [
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

export function filterMeasures(measures: T.MeasureEnhanced[]): T.MeasureEnhanced[] {
  return measures.filter(measure => !BANNED_MEASURES.includes(measure.metric.key));
}

export function sortMeasures(
  domainName: string,
  measures: Array<T.MeasureEnhanced | string>
): Array<T.MeasureEnhanced | string> {
  const config = domains[domainName] || {};
  const configOrder = config.order || [];
  return sortBy(measures, [
    (item: T.MeasureEnhanced | string) => {
      if (typeof item === 'string') {
        return configOrder.indexOf(item);
      }
      const idx = configOrder.indexOf(item.metric.key);
      return idx >= 0 ? idx : configOrder.length;
    },
    (item: T.MeasureEnhanced | string) =>
      typeof item === 'string' ? item : getLocalizedMetricName(item.metric)
  ]);
}

export function addMeasureCategories(domainName: string, measures: T.MeasureEnhanced[]) {
  const categories = domains[domainName] && domains[domainName].categories;
  if (categories && categories.length > 0) {
    return [...categories, ...measures];
  }
  return measures;
}

export function enhanceComponent(
  component: T.ComponentMeasure,
  metric: Pick<T.Metric, 'key'> | undefined,
  metrics: T.Dict<T.Metric>
): T.ComponentMeasureEnhanced {
  if (!component.measures) {
    return { ...component, measures: [] };
  }

  const enhancedMeasures = component.measures.map(measure => enhanceMeasure(measure, metrics));
  const measure = metric && enhancedMeasures.find(measure => measure.metric.key === metric.key);
  const value = measure && measure.value;
  const leak = measure && measure.leak;
  return { ...component, value, leak, measures: enhancedMeasures };
}

export function isFileType(component: T.ComponentMeasure): boolean {
  return ['FIL', 'UTS'].includes(component.qualifier);
}

export function isViewType(component: T.ComponentMeasure): boolean {
  return ['VW', 'SVW', 'APP'].includes(component.qualifier);
}

export const groupByDomains = memoize((measures: T.MeasureEnhanced[]) => {
  const domains = toPairs(groupBy(measures, measure => measure.metric.domain)).map(r => ({
    name: r[0],
    measures: r[1]
  }));

  return sortBy(domains, [
    (domain: { name: string; measures: T.MeasureEnhanced[] }) => {
      const idx = KNOWN_DOMAINS.indexOf(domain.name);
      return idx >= 0 ? idx : KNOWN_DOMAINS.length;
    },
    'name'
  ]);
});

export function hasList(metric: string): boolean {
  return !['releasability_rating', 'releasability_effort'].includes(metric);
}

export function hasTree(metric: string): boolean {
  return metric !== 'alert_status';
}

export function hasTreemap(metric: string, type: string): boolean {
  return ['PERCENT', 'RATING', 'LEVEL'].includes(type) && hasTree(metric);
}

export function hasBubbleChart(domainName: string): boolean {
  return bubbles[domainName] !== undefined;
}

export function hasFacetStat(metric: string): boolean {
  return metric !== 'alert_status';
}

export function hasFullMeasures(branch?: T.BranchLike) {
  return !branch || isLongLivingBranch(branch) || isMainBranch(branch);
}

export function getMeasuresPageMetricKeys(metrics: T.Dict<T.Metric>, branch?: T.BranchLike) {
  const metricKeys = getDisplayMetrics(Object.values(metrics)).map(metric => metric.key);

  if (isPullRequest(branch) || isShortLivingBranch(branch)) {
    return metricKeys.filter(key => isDiffMetric(key));
  } else {
    return metricKeys;
  }
}

export function getBubbleMetrics(domain: string, metrics: T.Dict<T.Metric>) {
  const conf = bubbles[domain];
  return {
    x: metrics[conf.x],
    y: metrics[conf.y],
    size: metrics[conf.size],
    colors: conf.colors && conf.colors.map(color => metrics[color])
  };
}

export function getBubbleYDomain(domain: string) {
  return bubbles[domain].yDomain;
}

export function isProjectOverview(metric: string) {
  return metric === PROJECT_OVERVEW;
}

function parseView(metric: string, rawView?: string): View {
  const view = (parseAsString(rawView) || DEFAULT_VIEW) as View;
  if (!hasTree(metric)) {
    return 'list';
  } else if (view === 'list' && !hasList(metric)) {
    return 'tree';
  }
  return view;
}

export interface Query {
  metric: string;
  selected?: string;
  view: View;
}

export const parseQuery = memoize(
  (urlQuery: T.RawQuery): Query => {
    const metric = parseAsString(urlQuery['metric']) || DEFAULT_METRIC;
    return {
      metric,
      selected: parseAsString(urlQuery['selected']),
      view: parseView(metric, urlQuery['view'])
    };
  }
);

export const serializeQuery = memoize((query: Query) => {
  return cleanQuery({
    metric: query.metric === DEFAULT_METRIC ? undefined : serializeString(query.metric),
    selected: serializeString(query.selected),
    view: query.view === DEFAULT_VIEW ? undefined : serializeString(query.view)
  });
});
