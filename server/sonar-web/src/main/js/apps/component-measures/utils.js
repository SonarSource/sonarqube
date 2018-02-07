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
// @flow
import { groupBy, memoize, sortBy, toPairs } from 'lodash';
import { domains } from './config/domains';
import { bubbles } from './config/bubbles';
import { getLocalizedMetricName } from '../../helpers/l10n';
import { cleanQuery, parseAsString, serializeString } from '../../helpers/query';
import { enhanceMeasure } from '../../components/measure/utils';
/*:: import type { Component, ComponentEnhanced, Query } from './types'; */
/*:: import type { RawQuery } from '../../helpers/query'; */
/*:: import type { Metric } from '../../store/metrics/actions'; */
/*:: import type { MeasureEnhanced } from '../../components/measure/types'; */

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

export function filterMeasures(
  measures /*: Array<MeasureEnhanced> */
) /*: Array<MeasureEnhanced> */ {
  return measures.filter(measure => !BANNED_MEASURES.includes(measure.metric.key));
}

export function sortMeasures(
  domainName /*: string */,
  measures /*: Array<MeasureEnhanced | string> */
) /*: Array<MeasureEnhanced | string> */ {
  const config = domains[domainName] || {};
  const configOrder = config.order || [];
  return sortBy(measures, [
    item => {
      if (typeof item === 'string') {
        return configOrder.indexOf(item);
      }
      const idx = configOrder.indexOf(item.metric.key);
      return idx >= 0 ? idx : configOrder.length;
    },
    item => (typeof item === 'string' ? item : getLocalizedMetricName(item.metric))
  ]);
}

export function addMeasureCategories(
  domainName /*: string */,
  measures /*: Array<MeasureEnhanced> */
) /*: Array<any> */ {
  const categories = domains[domainName] && domains[domainName].categories;
  if (categories && categories.length > 0) {
    return [...categories, ...measures];
  }
  return measures;
}

export function enhanceComponent(
  component /*: Component */,
  metric /*: ?Metric */,
  metrics /*: { [string]: Metric } */
) /*: ComponentEnhanced */ {
  const enhancedMeasures = component.measures.map(measure => enhanceMeasure(measure, metrics));
  // $FlowFixMe metric can't be null since there is a guard for it
  const measure = metric && enhancedMeasures.find(measure => measure.metric.key === metric.key);
  const value = measure ? measure.value : null;
  const leak = measure ? measure.leak : null;
  return { ...component, value, leak, measures: enhancedMeasures };
}

export function isFileType(component /*: Component */) /*: boolean */ {
  return ['FIL', 'UTS'].includes(component.qualifier);
}

export function isViewType(component /*: Component */) /*: boolean */ {
  return ['VW', 'SVW', 'APP'].includes(component.qualifier);
}

export const groupByDomains = memoize((measures /*: Array<MeasureEnhanced> */) => {
  const domains = toPairs(groupBy(measures, measure => measure.metric.domain)).map(r => ({
    name: r[0],
    measures: r[1]
  }));

  return sortBy(domains, [
    domain => {
      const idx = KNOWN_DOMAINS.indexOf(domain.name);
      return idx >= 0 ? idx : KNOWN_DOMAINS.length;
    },
    'name'
  ]);
});

export function getDefaultView(metric /*: string */) /*: string */ {
  if (!hasList(metric)) {
    return 'tree';
  }
  return DEFAULT_VIEW;
}

export function hasList(metric /*: string */) /*: boolean */ {
  return !['releasability_rating', 'releasability_effort'].includes(metric);
}

export function hasTree(metric /*: string */) /*: boolean */ {
  return metric !== 'alert_status';
}

export function hasTreemap(metric /*: string */, type /*: string */) /*: boolean */ {
  return ['PERCENT', 'RATING', 'LEVEL'].includes(type) && hasTree(metric);
}

export function hasBubbleChart(domainName /*: string */) /*: boolean */ {
  return bubbles[domainName] != null;
}

export function hasFacetStat(metric /*: string */) /*: boolean */ {
  return metric !== 'alert_status';
}

export function getBubbleMetrics(domain /*: string */, metrics /*: { [string]: Metric } */) {
  const conf = bubbles[domain];
  return {
    x: metrics[conf.x],
    y: metrics[conf.y],
    size: metrics[conf.size],
    colors: conf.colors ? conf.colors.map(color => metrics[color]) : null
  };
}

export function getBubbleYDomain(domain /*: string */) {
  return bubbles[domain].yDomain;
}

export function isProjectOverview(metric /*: string */) {
  return metric === PROJECT_OVERVEW;
}

const parseView = memoize((rawView /*:: ? */ /*: string */, metric /*: string */) => {
  const view = parseAsString(rawView) || DEFAULT_VIEW;
  if (!hasTree(metric)) {
    return 'list';
  } else if (view === 'list' && !hasList(metric)) {
    return 'tree';
  }
  return view;
});

export const parseQuery = memoize((urlQuery /*: RawQuery */) => {
  const metric = parseAsString(urlQuery['metric']) || DEFAULT_METRIC;
  return {
    metric,
    selected: parseAsString(urlQuery['selected']),
    view: parseView(urlQuery['view'], metric)
  };
});

export const serializeQuery = memoize((query /*: Query */) => {
  return cleanQuery({
    metric: query.metric === DEFAULT_METRIC ? null : serializeString(query.metric),
    selected: serializeString(query.selected),
    view: query.view === DEFAULT_VIEW ? null : serializeString(query.view)
  });
});
