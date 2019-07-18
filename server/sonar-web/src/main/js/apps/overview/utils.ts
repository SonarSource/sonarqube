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
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import CodeSmellIcon from 'sonar-ui-common/components/icons/CodeSmellIcon';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import DuplicationsRating from 'sonar-ui-common/components/ui/DuplicationsRating';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CoverageRating from '../../components/ui/CoverageRating';

export const METRICS = [
  // quality gate
  'alert_status',
  'quality_gate_details',

  // bugs
  'bugs',
  'new_bugs',
  'reliability_rating',
  'new_reliability_rating',

  // vulnerabilities
  'vulnerabilities',
  'new_vulnerabilities',
  'security_rating',
  'new_security_rating',
  'security_hotspots',
  'new_security_hotspots',

  // code smells
  'code_smells',
  'new_code_smells',
  'sqale_rating',
  'new_maintainability_rating',
  'sqale_index',
  'new_technical_debt',

  // coverage
  'coverage',
  'new_coverage',
  'new_lines_to_cover',
  'tests',

  // duplications
  'duplicated_lines_density',
  'new_duplicated_lines_density',
  'duplicated_blocks',

  // size
  'ncloc',
  'ncloc_language_distribution',
  'projects',
  'new_lines'
];

export const PR_METRICS = [
  'coverage',
  'new_coverage',
  'new_lines_to_cover',

  'duplicated_lines_density',
  'new_duplicated_lines_density',
  'new_lines',
  'new_code_smells',
  'new_maintainability_rating',
  'new_bugs',
  'new_reliability_rating',
  'new_vulnerabilities',
  'new_security_hotspots',
  'new_security_rating'
];

export const HISTORY_METRICS_LIST = [
  'bugs',
  'vulnerabilities',
  'sqale_index',
  'duplicated_lines_density',
  'ncloc',
  'coverage'
];

export type MeasurementType = 'COVERAGE' | 'DUPLICATION';

export const MEASUREMENTS_MAP = {
  COVERAGE: {
    metric: 'new_coverage',
    linesMetric: 'new_lines_to_cover',
    afterMergeMetric: 'coverage',
    labelKey: 'overview.metric.coverage',
    expandedLabelKey: 'overview.coverage_on_X_lines',
    iconClass: CoverageRating
  },
  DUPLICATION: {
    metric: 'new_duplicated_lines_density',
    linesMetric: 'new_lines',
    afterMergeMetric: 'duplicated_lines_density',
    labelKey: 'overview.metric.duplications',
    expandedLabelKey: 'overview.duplications_on_X',
    iconClass: DuplicationsRating
  }
};

export type IssueType = 'CODE_SMELL' | 'VULNERABILITY' | 'BUG' | 'SECURITY_HOTSPOT';

export const ISSUETYPE_MAP = {
  CODE_SMELL: {
    metric: 'new_code_smells',
    rating: 'new_maintainability_rating',
    ratingName: 'Maintainability',
    iconClass: CodeSmellIcon
  },
  VULNERABILITY: {
    metric: 'new_vulnerabilities',
    rating: 'new_security_rating',
    ratingName: 'Security',
    iconClass: VulnerabilityIcon
  },
  BUG: {
    metric: 'new_bugs',
    rating: 'new_reliability_rating',
    ratingName: 'Reliability',
    iconClass: BugIcon
  },
  SECURITY_HOTSPOT: {
    metric: 'new_security_hotspots',
    rating: '',
    ratingName: '',
    iconClass: SecurityHotspotIcon
  }
};

export function getMetricName(metricKey: string) {
  return translate('overview.metric', metricKey);
}

export function getRatingName(type: IssueType) {
  return translate('metric_domain', ISSUETYPE_MAP[type].ratingName);
}
