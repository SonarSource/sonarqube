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
export const PORTFOLIO_METRICS = [
  'projects',
  'ncloc',
  'ncloc_language_distribution',

  'releasability_rating',
  'releasability_effort',

  'sqale_rating',
  'maintainability_rating_effort',

  'reliability_rating',
  'reliability_rating_effort',

  'security_rating',
  'security_rating_effort',

  'security_review_rating',
  'security_review_rating_effort',

  'last_change_on_releasability_rating',
  'last_change_on_maintainability_rating',
  'last_change_on_security_rating',
  'last_change_on_security_review_rating',
  'last_change_on_reliability_rating'
];

export interface MetricKeys {
  activity?: string;
  effort: string;
  measuresMetric: string;
  label: string;
  last_change: string;
  rating: string;
}

export const METRICS_PER_TYPE: T.Dict<MetricKeys> = {
  releasability: {
    measuresMetric: 'Releasability',
    label: 'metric_domain.Releasability',
    rating: 'releasability_rating',
    effort: 'releasability_effort',
    last_change: 'last_change_on_releasability_rating'
  },
  reliability: {
    measuresMetric: 'Reliability',
    label: 'metric_domain.Reliability',
    rating: 'reliability_rating',
    effort: 'reliability_rating_effort',
    last_change: 'last_change_on_reliability_rating'
  },
  vulnerabilities: {
    measuresMetric: 'Security',
    label: 'portfolio.metric_domain.vulnerabilities',
    rating: 'security_rating',
    effort: 'security_rating_effort',
    last_change: 'last_change_on_security_rating',
    activity: 'security_rating,vulnerabilities'
  },
  security_hotspots: {
    measuresMetric: 'security_review_rating',
    label: 'portfolio.metric_domain.security_hotspots',
    rating: 'security_review_rating',
    effort: 'security_review_rating_effort',
    last_change: 'last_change_on_security_review_rating'
  },
  maintainability: {
    measuresMetric: 'Maintainability',
    label: 'metric_domain.Maintainability',
    rating: 'sqale_rating',
    effort: 'maintainability_rating_effort',
    last_change: 'last_change_on_maintainability_rating'
  }
};

export const SUB_COMPONENTS_METRICS = [
  'ncloc',
  'releasability_rating',
  'security_rating',
  'security_review_rating',
  'reliability_rating',
  'sqale_rating',
  'alert_status'
];

export function convertMeasures(measures: Array<{ metric: string; value?: string }>) {
  const result: T.Dict<string | undefined> = {};
  measures.forEach(measure => {
    result[measure.metric] = measure.value;
  });
  return result;
}
