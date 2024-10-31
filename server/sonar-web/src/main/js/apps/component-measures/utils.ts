/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { isBranch, isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { RawQuery } from '~sonar-aligned/types/router';
import { enhanceMeasure } from '../../components/measure/utils';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  HIDDEN_METRICS,
  LEAK_CCT_SOFTWARE_QUALITY_METRICS,
  LEAK_OLD_TAXONOMY_METRICS,
  LEAK_OLD_TAXONOMY_RATINGS,
  OLD_TAXONOMY_METRICS,
  OLD_TAXONOMY_RATINGS,
  SOFTWARE_QUALITY_RATING_METRICS,
} from '../../helpers/constants';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
import {
  areCCTMeasuresComputed,
  areLeakCCTMeasuresComputed,
  areSoftwareQualityRatingsComputed,
  getDisplayMetrics,
  isDiffMetric,
  MEASURES_REDIRECTION,
} from '../../helpers/measures';
import {
  cleanQuery,
  parseAsOptionalBoolean,
  parseAsString,
  serializeString,
} from '../../helpers/query';
import { BranchLike } from '../../types/branch-like';
import { Domain, MeasurePageView } from '../../types/measures';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  Dict,
  Measure,
  MeasureEnhanced,
  Metric,
} from '../../types/types';
import { BubblesByDomain } from './config/bubbles';
import { domains } from './config/domains';

export const BUBBLES_FETCH_LIMIT = 500;
export const PROJECT_OVERVIEW = 'project_overview';
export const DEFAULT_VIEW = MeasurePageView.tree;
export const DEFAULT_METRIC = PROJECT_OVERVIEW;
export const KNOWN_DOMAINS = [
  'Releasability',
  'Security',
  'Reliability',
  'Maintainability',
  'SecurityReview',
  'Coverage',
  'Duplications',
  'Size',
  'Complexity',
];

const DEPRECATED_METRICS = [
  MetricKey.blocker_violations,
  MetricKey.new_blocker_violations,
  MetricKey.critical_violations,
  MetricKey.new_critical_violations,
  MetricKey.major_violations,
  MetricKey.new_major_violations,
  MetricKey.info_violations,
  MetricKey.new_info_violations,
  MetricKey.minor_violations,
  MetricKey.new_minor_violations,
  MetricKey.high_impact_accepted_issues,
];

const ISSUES_METRICS = [
  MetricKey.accepted_issues,
  MetricKey.new_accepted_issues,
  MetricKey.confirmed_issues,
  MetricKey.false_positive_issues,
  MetricKey.violations,
  MetricKey.new_violations,
];

export const populateDomainsFromMeasures = memoize(
  (measures: MeasureEnhanced[], isStandardMode = false): Domain[] => {
    let populatedMeasures = measures
      .filter((measure) => !DEPRECATED_METRICS.includes(measure.metric.key as MetricKey))
      .map((measure) => {
        const isDiff = isDiffMetric(measure.metric.key);
        const calculatedValue = isDiff ? measure.leak : measure.value;

        return {
          ...measure,
          ...{ [isDiff ? 'leak' : 'value']: calculatedValue },
        };
      });

    if (!isStandardMode && areLeakCCTMeasuresComputed(measures)) {
      populatedMeasures = populatedMeasures.filter(
        (measure) => !LEAK_OLD_TAXONOMY_METRICS.includes(measure.metric.key as MetricKey),
      );
    } else {
      populatedMeasures = populatedMeasures.filter(
        (measure) => !LEAK_CCT_SOFTWARE_QUALITY_METRICS.includes(measure.metric.key as MetricKey),
      );
    }

    // Both new and overall code will exist after next analysis
    if (!isStandardMode && areSoftwareQualityRatingsComputed(measures)) {
      populatedMeasures = populatedMeasures.filter(
        (measure) =>
          !OLD_TAXONOMY_RATINGS.includes(measure.metric.key as MetricKey) &&
          !LEAK_OLD_TAXONOMY_RATINGS.includes(measure.metric.key as MetricKey),
      );
    } else {
      populatedMeasures = populatedMeasures.filter(
        (measure) => !SOFTWARE_QUALITY_RATING_METRICS.includes(measure.metric.key as MetricKey),
      );
    }

    if (!isStandardMode && areCCTMeasuresComputed(measures)) {
      populatedMeasures = populatedMeasures.filter(
        (measure) => !OLD_TAXONOMY_METRICS.includes(measure.metric.key as MetricKey),
      );
    } else {
      populatedMeasures = populatedMeasures.filter(
        (measure) => !CCT_SOFTWARE_QUALITY_METRICS.includes(measure.metric.key as MetricKey),
      );
    }

    return groupByDomains(populatedMeasures);
  },
);

export function getMetricSubnavigationName(
  metric: Metric,
  translateFn: (metric: Metric) => string,
  isDiff = false,
  isStandardMode = false,
) {
  // MQR mode and old taxonomy metrics, we return "Issues" for them anyway
  if (!isStandardMode && OLD_TAXONOMY_METRICS.includes(metric.key as MetricKey)) {
    return translate('component_measures.awaiting_analysis.name');
  }
  if (!isStandardMode && LEAK_OLD_TAXONOMY_METRICS.includes(metric.key as MetricKey)) {
    return translate('component_measures.leak_awaiting_analysis.name');
  }

  if (
    [
      ...LEAK_CCT_SOFTWARE_QUALITY_METRICS,
      ...CCT_SOFTWARE_QUALITY_METRICS,
      ...ISSUES_METRICS,
      ...OLD_TAXONOMY_METRICS,
      ...LEAK_OLD_TAXONOMY_METRICS,
    ].includes(metric.key as MetricKey)
  ) {
    return translate(
      `component_measures.metric.${metric.key}.${isDiff ? 'detailed_name' : 'name'}`,
    );
  }
  return translateFn(metric);
}

export function filterMeasures(measures: MeasureEnhanced[]): MeasureEnhanced[] {
  return measures.filter((measure) => !HIDDEN_METRICS.includes(measure.metric.key as MetricKey));
}

export function sortMeasures(
  domainName: string,
  measures: Array<MeasureEnhanced | string>,
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
      typeof item === 'string' ? item : getLocalizedMetricName(item.metric),
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
  metric: Pick<Metric, 'key'> | undefined,
  metrics: Dict<Metric>,
): ComponentMeasureEnhanced {
  if (!component.measures) {
    return { ...component, measures: [] };
  }

  const enhancedMeasures = component.measures.map((measure) => enhanceMeasure(measure, metrics));
  const measure = metric && enhancedMeasures.find((measure) => measure.metric.key === metric.key);
  const value = measure && measure.value;
  const leak = measure && measure.leak;
  return { ...component, value, leak, measures: enhancedMeasures };
}

export function isSecurityReviewMetric(metricKey: MetricKey | string): boolean {
  return [
    MetricKey.security_hotspots,
    MetricKey.security_hotspots_reviewed,
    MetricKey.security_review_rating,
    MetricKey.new_security_hotspots,
    MetricKey.new_security_hotspots_reviewed,
    MetricKey.new_security_review_rating,
  ].includes(metricKey as MetricKey);
}

export function banQualityGateMeasure({ measures = [], qualifier }: ComponentMeasure): Measure[] {
  const bannedMetrics: string[] = [];
  if (ComponentQualifier.Portfolio !== qualifier && ComponentQualifier.SubPortfolio !== qualifier) {
    bannedMetrics.push(MetricKey.alert_status);
  }
  if (qualifier === ComponentQualifier.Application) {
    bannedMetrics.push(MetricKey.releasability_rating, MetricKey.releasability_effort);
  }
  return measures.filter((measure) => !bannedMetrics.includes(measure.metric));
}

export const groupByDomains = memoize((measures: MeasureEnhanced[]) => {
  const domains = toPairs(groupBy(measures, (measure) => measure.metric.domain)).map((r) => ({
    name: r[0],
    measures: r[1],
  }));

  return sortBy(domains, [
    (domain: { measures: MeasureEnhanced[]; name: string }) => {
      const idx = KNOWN_DOMAINS.indexOf(domain.name);
      return idx >= 0 ? idx : KNOWN_DOMAINS.length;
    },
    'name',
  ]);
});

export function hasList(metric: string): boolean {
  return ![MetricKey.releasability_rating, MetricKey.releasability_effort].includes(
    metric as MetricKey,
  );
}

export function hasTree(metric: string): boolean {
  return metric !== MetricKey.alert_status;
}

export function hasTreemap(metric: string, type: string): boolean {
  return (
    [MetricType.Percent, MetricType.Rating, MetricType.Level].includes(type as MetricType) &&
    hasTree(metric)
  );
}

export function hasBubbleChart(bubblesByDomain: BubblesByDomain, domainName: string): boolean {
  return bubblesByDomain[domainName] !== undefined;
}

export function hasFacetStat(metric: string): boolean {
  return metric !== MetricKey.alert_status;
}

export function hasFullMeasures(branch?: BranchLike) {
  return !branch || isBranch(branch);
}

export function getMeasuresPageMetricKeys(metrics: Dict<Metric>, branch?: BranchLike) {
  const metricKeys = getDisplayMetrics(Object.values(metrics)).map((metric) => metric.key);

  if (isPullRequest(branch)) {
    return metricKeys.filter((key) => isDiffMetric(key));
  }

  return metricKeys;
}

export function getBubbleMetrics(
  bubblesByDomain: BubblesByDomain,
  domain: string,
  metrics: Dict<Metric>,
) {
  const conf = bubblesByDomain[domain];
  return {
    x: metrics[conf.x],
    y: metrics[conf.y],
    size: metrics[conf.size],
    colors: conf.colors && conf.colors.map((color) => metrics[color]),
  };
}

export function getBubbleYDomain(bubblesByDomain: BubblesByDomain, domain: string) {
  return bubblesByDomain[domain].yDomain;
}

export function isProjectOverview(metric: string) {
  return metric === PROJECT_OVERVIEW;
}

function parseView(metric: MetricKey, rawView?: string): MeasurePageView {
  const view = (parseAsString(rawView) || DEFAULT_VIEW) as MeasurePageView;
  if (!hasTree(metric)) {
    return MeasurePageView.list;
  } else if (view === MeasurePageView.list && !hasList(metric)) {
    return MeasurePageView.tree;
  }
  return view;
}

export interface Query {
  asc?: boolean;
  metric: string;
  selected?: string;
  view: MeasurePageView;
}

export const parseQuery = memoize((urlQuery: RawQuery): Query => {
  const parsedMetric = parseAsString<MetricKey>(urlQuery['metric']) || DEFAULT_METRIC;
  const metric = MEASURES_REDIRECTION[parsedMetric] ?? parsedMetric;
  return {
    metric,
    selected: parseAsString(urlQuery['selected']),
    view: parseView(metric, urlQuery['view']),
    asc: parseAsOptionalBoolean(urlQuery['asc']),
  };
});

export const serializeQuery = memoize((query: Query) => {
  return cleanQuery({
    metric: query.metric === DEFAULT_METRIC ? undefined : serializeString(query.metric),
    selected: serializeString(query.selected),
    view: query.view === DEFAULT_VIEW ? undefined : serializeString(query.view),
  });
});
