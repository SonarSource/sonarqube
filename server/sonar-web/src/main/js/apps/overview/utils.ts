/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import CodeSmellIcon from 'sonar-ui-common/components/icons/CodeSmellIcon';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import DuplicationsRating from 'sonar-ui-common/components/ui/DuplicationsRating';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CoverageRating from '../../components/ui/CoverageRating';
import { MetricKey } from '../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../types/quality-gates';

export const METRICS: string[] = [
  // quality gate
  MetricKey.alert_status,
  MetricKey.quality_gate_details, // TODO: still relevant?

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
  MetricKey.new_lines
];

export const PR_METRICS: string[] = [
  MetricKey.coverage,
  MetricKey.new_coverage,
  MetricKey.new_lines_to_cover,

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
  MetricKey.new_security_rating
];

export const HISTORY_METRICS_LIST: string[] = [
  MetricKey.bugs,
  MetricKey.vulnerabilities,
  MetricKey.sqale_index,
  MetricKey.duplicated_lines_density,
  MetricKey.ncloc,
  MetricKey.coverage
];

export enum MeasurementType {
  Coverage = 'COVERAGE',
  Duplication = 'DUPLICATION'
}

const MEASUREMENTS_MAP = {
  [MeasurementType.Coverage]: {
    metric: MetricKey.coverage,
    newMetric: MetricKey.new_coverage,
    linesMetric: MetricKey.lines_to_cover,
    newLinesMetric: MetricKey.new_lines_to_cover,
    afterMergeMetric: MetricKey.coverage,
    labelKey: 'metric.coverage.name',
    expandedLabelKey: 'overview.coverage_on_X_lines',
    newLinesExpandedLabelKey: 'overview.coverage_on_X_new_lines',
    iconClass: CoverageRating
  },
  [MeasurementType.Duplication]: {
    metric: MetricKey.duplicated_lines_density,
    newMetric: MetricKey.new_duplicated_lines_density,
    linesMetric: MetricKey.ncloc,
    newLinesMetric: MetricKey.new_lines,
    afterMergeMetric: MetricKey.duplicated_lines_density,
    labelKey: 'metric.duplicated_lines_density.short_name',
    expandedLabelKey: 'overview.duplications_on_X_lines',
    newLinesExpandedLabelKey: 'overview.duplications_on_X_new_lines',
    iconClass: DuplicationsRating
  }
};

export enum IssueType {
  CodeSmell = 'CODE_SMELL',
  Vulnerability = 'VULNERABILITY',
  Bug = 'BUG',
  SecurityHotspot = 'SECURITY_HOTSPOT'
}

const ISSUETYPE_MAP = {
  [IssueType.CodeSmell]: {
    metric: MetricKey.code_smells,
    newMetric: MetricKey.new_code_smells,
    rating: MetricKey.sqale_rating,
    newRating: MetricKey.new_maintainability_rating,
    ratingName: 'Maintainability',
    iconClass: CodeSmellIcon
  },
  [IssueType.Vulnerability]: {
    metric: MetricKey.vulnerabilities,
    newMetric: MetricKey.new_vulnerabilities,
    rating: MetricKey.security_rating,
    newRating: MetricKey.new_security_rating,
    ratingName: 'Security',
    iconClass: VulnerabilityIcon
  },
  [IssueType.Bug]: {
    metric: MetricKey.bugs,
    newMetric: MetricKey.new_bugs,
    rating: MetricKey.reliability_rating,
    newRating: MetricKey.new_reliability_rating,
    ratingName: 'Reliability',
    iconClass: BugIcon
  },
  [IssueType.SecurityHotspot]: {
    metric: MetricKey.security_hotspots,
    newMetric: MetricKey.new_security_hotspots,
    rating: MetricKey.security_review_rating,
    newRating: MetricKey.new_security_review_rating,
    ratingName: 'SecurityReview',
    iconClass: SecurityHotspotIcon
  }
};

export function getIssueRatingName(type: IssueType) {
  return translate('metric_domain', ISSUETYPE_MAP[type].ratingName);
}

export function getIssueIconClass(type: IssueType) {
  return ISSUETYPE_MAP[type].iconClass;
}

export function getIssueMetricKey(type: IssueType, useDiffMetric: boolean) {
  return useDiffMetric ? ISSUETYPE_MAP[type].newMetric : ISSUETYPE_MAP[type].metric;
}

export function getIssueRatingMetricKey(type: IssueType, useDiffMetric: boolean) {
  return useDiffMetric ? ISSUETYPE_MAP[type].newRating : ISSUETYPE_MAP[type].rating;
}

export function getMeasurementIconClass(type: MeasurementType) {
  return MEASUREMENTS_MAP[type].iconClass;
}

export function getMeasurementMetricKey(type: MeasurementType, useDiffMetric: boolean) {
  return useDiffMetric ? MEASUREMENTS_MAP[type].newMetric : MEASUREMENTS_MAP[type].metric;
}

export function getMeasurementAfterMergeMetricKey(type: MeasurementType) {
  return MEASUREMENTS_MAP[type].afterMergeMetric;
}

export function getMeasurementLinesMetricKey(type: MeasurementType, useDiffMetric: boolean) {
  return useDiffMetric ? MEASUREMENTS_MAP[type].newLinesMetric : MEASUREMENTS_MAP[type].linesMetric;
}

export function getMeasurementLabelKeys(type: MeasurementType, useDiffMetric: boolean) {
  return {
    expandedLabelKey: useDiffMetric
      ? MEASUREMENTS_MAP[type].newLinesExpandedLabelKey
      : MEASUREMENTS_MAP[type].expandedLabelKey,
    labelKey: MEASUREMENTS_MAP[type].labelKey
  };
}

/*
 * Extract a specific metric's threshold from the quality gate details
 */
export function getThreshold(
  measures: T.MeasureEnhanced[],
  metricKey: MetricKey | string
): number | undefined {
  const detailsMeasure = measures.find(
    measure => measure.metric.key === MetricKey.quality_gate_details
  );
  if (detailsMeasure && detailsMeasure.value) {
    const details = safeParse(detailsMeasure.value);
    const conditions: QualityGateStatusConditionEnhanced[] = details.conditions || [];

    const condition = conditions.find(c => c.metric === metricKey);
    if (condition) {
      return parseFloat(
        (condition.level === 'ERROR' ? condition.error : condition.warning) as string
      );
    }
  }
  return undefined;
}

function safeParse(json: string) {
  try {
    return JSON.parse(json);
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error(e);
    return {};
  }
}
