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
import { memoize } from 'lodash';
import React from 'react';
import { IntlShape } from 'react-intl';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { RawQuery } from '~sonar-aligned/types/router';
import { ISSUETYPE_METRIC_KEYS_MAP } from '../../helpers/issues';
import { translate } from '../../helpers/l10n';
import { parseAsString } from '../../helpers/query';
import { SoftwareQuality } from '../../types/clean-code-taxonomy';
import { IssueType } from '../../types/issues';
import { MetricKey, MetricType } from '../../types/metrics';
import { AnalysisMeasuresVariations, MeasureHistory } from '../../types/project-activity';
import { QualityGateStatusConditionEnhanced } from '../../types/quality-gates';
import { Dict } from '../../types/types';

export const BRANCH_OVERVIEW_METRICS: string[] = [
  // quality gate
  MetricKey.alert_status,
  MetricKey.quality_gate_details, // TODO: still relevant?

  // issues
  MetricKey.new_violations,
  MetricKey.accepted_issues,
  MetricKey.new_accepted_issues,
  MetricKey.high_impact_accepted_issues,
  MetricKey.maintainability_issues,
  MetricKey.reliability_issues,
  MetricKey.security_issues,

  // bugs
  MetricKey.bugs,
  MetricKey.new_bugs,
  MetricKey.reliability_rating,
  MetricKey.new_reliability_rating,

  // vulnerabilities
  MetricKey.vulnerabilities,
  MetricKey.new_vulnerabilities,
  MetricKey.security_rating,
  MetricKey.new_security_rating,

  // hotspots
  MetricKey.security_hotspots,
  MetricKey.new_security_hotspots,
  MetricKey.security_hotspots_reviewed,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.security_review_rating,
  MetricKey.new_security_review_rating,

  // code smells
  MetricKey.code_smells,
  MetricKey.new_code_smells,
  MetricKey.sqale_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.sqale_index,
  MetricKey.new_technical_debt,

  // coverage
  MetricKey.coverage,
  MetricKey.new_coverage,
  MetricKey.lines_to_cover,
  MetricKey.new_lines_to_cover,
  MetricKey.tests,

  // duplications
  MetricKey.duplicated_lines_density,
  MetricKey.new_duplicated_lines_density,
  MetricKey.duplicated_blocks,

  // size
  MetricKey.ncloc,
  MetricKey.ncloc_language_distribution,
  MetricKey.projects,
  MetricKey.lines,
  MetricKey.new_lines,
];

export const PR_METRICS: string[] = [
  MetricKey.coverage,
  MetricKey.new_coverage,
  MetricKey.new_lines_to_cover,

  MetricKey.new_accepted_issues,
  MetricKey.new_violations,
  MetricKey.duplicated_lines_density,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.new_code_smells,
  MetricKey.new_maintainability_rating,
  MetricKey.new_bugs,
  MetricKey.new_reliability_rating,
  MetricKey.new_vulnerabilities,
  MetricKey.new_security_hotspots,
  MetricKey.new_security_review_rating,
  MetricKey.new_security_rating,

  MetricKey.pull_request_fixed_issues,
];

export const HISTORY_METRICS_LIST: string[] = [
  MetricKey.violations,
  MetricKey.duplicated_lines_density,
  MetricKey.ncloc,
  MetricKey.coverage,
  MetricKey.alert_status,
];

const MEASURES_VARIATIONS_METRICS = [
  MetricKey.bugs,
  MetricKey.code_smells,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
  MetricKey.vulnerabilities,
];

export enum MeasurementType {
  Coverage = 'COVERAGE',
  Duplication = 'DUPLICATION',
}

export enum Status {
  OK = 'OK',
  ERROR = 'ERROR',
}

const MEASUREMENTS_MAP = {
  [MeasurementType.Coverage]: {
    metric: MetricKey.coverage,
    newMetric: MetricKey.new_coverage,
  },
  [MeasurementType.Duplication]: {
    metric: MetricKey.duplicated_lines_density,
    newMetric: MetricKey.new_duplicated_lines_density,
  },
};

export const RATING_TO_SEVERITIES_MAPPING = [
  'BLOCKER,CRITICAL,MAJOR,MINOR',
  'BLOCKER,CRITICAL,MAJOR',
  'BLOCKER,CRITICAL',
  'BLOCKER',
];

export const RATING_METRICS_MAPPING: Dict<IssueType> = {
  [MetricKey.reliability_rating]: IssueType.Bug,
  [MetricKey.new_reliability_rating]: IssueType.Bug,
  [MetricKey.security_rating]: IssueType.Vulnerability,
  [MetricKey.new_security_rating]: IssueType.Vulnerability,
  [MetricKey.sqale_rating]: IssueType.CodeSmell,
  [MetricKey.new_maintainability_rating]: IssueType.CodeSmell,
  [MetricKey.security_review_rating]: IssueType.SecurityHotspot,
  [MetricKey.new_security_review_rating]: IssueType.SecurityHotspot,
};

export const METRICS_REPORTED_IN_OVERVIEW_CARDS = [
  MetricKey.new_violations,
  MetricKey.violations,
  MetricKey.new_coverage,
  MetricKey.coverage,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.security_hotspots_reviewed,
  MetricKey.new_duplicated_lines_density,
  MetricKey.duplicated_lines_density,
];

export function softwareQualityToMeasure(softwareQuality: SoftwareQuality): MetricKey {
  return (softwareQuality.toLowerCase() + '_issues') as MetricKey;
}

export function getIssueRatingName(type: IssueType) {
  return translate('metric_domain', ISSUETYPE_METRIC_KEYS_MAP[type].ratingName);
}

export function getIssueMetricKey(type: IssueType, useDiffMetric: boolean) {
  return useDiffMetric
    ? ISSUETYPE_METRIC_KEYS_MAP[type].newMetric
    : ISSUETYPE_METRIC_KEYS_MAP[type].metric;
}

export function getIssueRatingMetricKey(type: IssueType, useDiffMetric: boolean) {
  return useDiffMetric
    ? ISSUETYPE_METRIC_KEYS_MAP[type].newRating
    : ISSUETYPE_METRIC_KEYS_MAP[type].rating;
}

export function getMeasurementMetricKey(type: MeasurementType, useDiffMetric: boolean) {
  return useDiffMetric ? MEASUREMENTS_MAP[type].newMetric : MEASUREMENTS_MAP[type].metric;
}

export const parseQuery = memoize((urlQuery: RawQuery): { codeScope: string } => {
  return {
    codeScope: parseAsString(urlQuery['code_scope']),
  };
});

export function getAnalysisVariations(measures: MeasureHistory[], analysesCount: number) {
  if (analysesCount === 0) {
    return [];
  }

  const emptyVariations: AnalysisMeasuresVariations[] = Array.from(
    { length: analysesCount },
    () => ({}),
  );

  return measures.reduce((variations, { metric, history }) => {
    if (!MEASURES_VARIATIONS_METRICS.includes(metric)) {
      return variations;
    }

    history.slice(-analysesCount).forEach(({ value = '' }, index, analysesHistory) => {
      if (index === 0) {
        variations[index][metric] = parseFloat(value) || 0;
        return;
      }

      const previousValue = parseFloat(analysesHistory[index - 1].value ?? '') || 0;
      const numericValue = parseFloat(value) || 0;
      const variation = numericValue - previousValue;

      if (variation === 0) {
        return;
      }

      variations[index][metric] = variation;
    });

    return variations;
  }, emptyVariations);
}

export function getConditionRequiredTranslateId(metric: MetricKey) {
  if (
    [MetricKey.security_hotspots_reviewed, MetricKey.new_security_hotspots_reviewed].includes(
      metric,
    )
  ) {
    return 'overview.quality_gate.required_x_reviewed';
  }

  return 'overview.quality_gate.required_x';
}

export function getConditionRequiredLabel(
  condition: QualityGateStatusConditionEnhanced,
  intl: IntlShape,
  failed = false,
) {
  let operator = condition.op === 'GT' ? '≤' : '≥';

  if (operator === '≤' && condition.error === '0') {
    operator = '=';
  }

  if (
    operator === '≥' &&
    condition.error === '100' &&
    condition.measure.metric.type === MetricType.Percent
  ) {
    operator = '=';
  }

  const conditionEl = formatMeasure(condition.error, condition.measure.metric.type, {
    decimals: 2,
    omitExtraDecimalZeros: true,
  });

  return intl.formatMessage(
    { id: getConditionRequiredTranslateId(condition.metric) },

    {
      operator,
      requirement: failed ? <b>{conditionEl}</b> : conditionEl,
    },
  );
}
