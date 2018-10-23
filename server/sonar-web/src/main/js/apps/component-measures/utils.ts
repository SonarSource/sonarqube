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
import { groupBy, memoize, sortBy, toPairs } from 'lodash';
import { domains } from './config/domains';
import { bubbles } from './config/bubbles';
import { getLocalizedMetricName } from '../../helpers/l10n';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  Metric,
  MeasureEnhanced,
  BranchLike
} from '../../app/types';
import { enhanceMeasure } from '../../components/measure/utils';
import { cleanQuery, parseAsString, RawQuery, serializeString } from '../../helpers/query';
import { isLongLivingBranch, isMainBranch } from '../../helpers/branches';
import { getDisplayMetrics } from '../../helpers/measures';

export const PROJECT_OVERVEW = 'project_overview';
export const DEFAULT_VIEW = 'list';
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

export function filterMeasures(measures: MeasureEnhanced[]): MeasureEnhanced[] {
  return measures.filter(measure => !BANNED_MEASURES.includes(measure.metric.key));
}

export function sortMeasures(
  domainName: string,
  measures: Array<MeasureEnhanced | string>
): Array<MeasureEnhanced | string> {
  const config = domains[domainName] || {};
  const configOrder = config.order || [];
  return sortBy(measures, [
    (item: MeasureEnhanced | string) => {
      if (typeof item === 'string') {
        return configOrder.indexOf(item);
      }
      const idx = configOrder.indexOf(item.metric.key);
      return idx >= 0 ? idx : configOrder.length;
    },
    (item: MeasureEnhanced | string) =>
      typeof item === 'string' ? item : getLocalizedMetricName(item.metric)
  ]);
}

export function addMeasureCategories(domainName: string, measures: MeasureEnhanced[]) {
  const categories = domains[domainName] && domains[domainName].categories;
  if (categories && categories.length > 0) {
    return [...categories, ...measures];
  }
  return measures;
}

export function enhanceComponent(
  component: ComponentMeasure,
  metric: Metric | undefined,
  metrics: { [key: string]: Metric }
): ComponentMeasureEnhanced {
  if (!component.measures) {
    return { ...component, measures: [] };
  }

  const enhancedMeasures = component.measures.map(measure => enhanceMeasure(measure, metrics));
  const measure = metric && enhancedMeasures.find(measure => measure.metric.key === metric.key);
  const value = measure && measure.value;
  const leak = measure && measure.leak;
  return { ...component, value, leak, measures: enhancedMeasures };
}

export function isFileType(component: ComponentMeasure): boolean {
  return ['FIL', 'UTS'].includes(component.qualifier);
}

export function isViewType(component: ComponentMeasure): boolean {
  return ['VW', 'SVW', 'APP'].includes(component.qualifier);
}

export const groupByDomains = memoize((measures: MeasureEnhanced[]) => {
  const domains = toPairs(groupBy(measures, measure => measure.metric.domain)).map(r => ({
    name: r[0],
    measures: r[1]
  }));

  return sortBy(domains, [
    (domain: { name: string; measures: MeasureEnhanced[] }) => {
      const idx = KNOWN_DOMAINS.indexOf(domain.name);
      return idx >= 0 ? idx : KNOWN_DOMAINS.length;
    },
    'name'
  ]);
});

export function getDefaultView(metric: string): string {
  if (!hasList(metric)) {
    return 'tree';
  }
  return DEFAULT_VIEW;
}

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

export function hasFullMeasures(branch?: BranchLike) {
  return !branch || isLongLivingBranch(branch) || isMainBranch(branch);
}

export function getMeasuresPageMetricKeys(metrics: { [key: string]: Metric }, branch?: BranchLike) {
  if (!hasFullMeasures(branch)) {
    return [
      'coverage',
      'new_coverage',
      'new_lines_to_cover',
      'new_uncovered_lines',
      'new_line_coverage',
      'new_conditions_to_cover',
      'new_uncovered_conditions',
      'new_branch_coverage',

      'duplicated_lines_density',
      'new_duplicated_lines_density',
      'new_duplicated_lines',
      'new_duplicated_blocks'
    ];
  }

  return getDisplayMetrics(Object.values(metrics)).map(metric => metric.key);
}

export function getBubbleMetrics(domain: string, metrics: { [key: string]: Metric }) {
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

const parseView = (metric: string, rawView?: string) => {
  const view = parseAsString(rawView) || DEFAULT_VIEW;
  if (!hasTree(metric)) {
    return 'list';
  } else if (view === 'list' && !hasList(metric)) {
    return 'tree';
  }
  return view;
};

export interface Query {
  metric: string;
  selected?: string;
  view: string;
}

export const parseQuery = memoize((urlQuery: RawQuery) => {
  const metric = parseAsString(urlQuery['metric']) || DEFAULT_METRIC;
  return {
    metric,
    selected: parseAsString(urlQuery['selected']),
    view: parseView(metric, urlQuery['view'])
  };
});

export const serializeQuery = memoize((query: Query) => {
  return cleanQuery({
    metric: query.metric === DEFAULT_METRIC ? null : serializeString(query.metric),
    selected: serializeString(query.selected),
    view: query.view === DEFAULT_VIEW ? null : serializeString(query.view)
  });
});
