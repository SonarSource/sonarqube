/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { IssueSimpleStatus } from '../../types/issues';
import { MetricKey } from '../../types/metrics';
import { Dict } from '../../types/types';

const ISSUE_MEASURES = [
  MetricKey.violations,
  MetricKey.new_violations,
  MetricKey.blocker_violations,
  MetricKey.critical_violations,
  MetricKey.major_violations,
  MetricKey.minor_violations,
  MetricKey.info_violations,
  MetricKey.new_blocker_violations,
  MetricKey.new_critical_violations,
  MetricKey.new_major_violations,
  MetricKey.new_minor_violations,
  MetricKey.new_info_violations,
  MetricKey.open_issues,
  MetricKey.reopened_issues,
  MetricKey.confirmed_issues,
  MetricKey.false_positive_issues,
  MetricKey.code_smells,
  MetricKey.new_code_smells,
  MetricKey.bugs,
  MetricKey.new_bugs,
  MetricKey.vulnerabilities,
  MetricKey.new_vulnerabilities,
];

export const DEFAULT_ISSUES_QUERY = {
  simpleStatuses: `${IssueSimpleStatus.Open},${IssueSimpleStatus.Confirmed}`,
};

const issueParamsPerMetric: Dict<Dict<string>> = {
  [MetricKey.blocker_violations]: { severities: 'BLOCKER' },
  [MetricKey.new_blocker_violations]: { severities: 'BLOCKER' },
  [MetricKey.critical_violations]: { severities: 'CRITICAL' },
  [MetricKey.new_critical_violations]: { severities: 'CRITICAL' },
  [MetricKey.major_violations]: { severities: 'MAJOR' },
  [MetricKey.new_major_violations]: { severities: 'MAJOR' },
  [MetricKey.minor_violations]: { severities: 'MINOR' },
  [MetricKey.new_minor_violations]: { severities: 'MINOR' },
  [MetricKey.info_violations]: { severities: 'INFO' },
  [MetricKey.new_info_violations]: { severities: 'INFO' },
  [MetricKey.open_issues]: { simpleStatuses: IssueSimpleStatus.Open },
  [MetricKey.reopened_issues]: { simpleStatuses: IssueSimpleStatus.Open },
  [MetricKey.confirmed_issues]: { simpleStatuses: IssueSimpleStatus.Confirmed },
  [MetricKey.false_positive_issues]: { simpleStatuses: IssueSimpleStatus.FalsePositive },
  [MetricKey.code_smells]: { types: 'CODE_SMELL' },
  [MetricKey.new_code_smells]: { types: 'CODE_SMELL' },
  [MetricKey.bugs]: { types: 'BUG' },
  [MetricKey.new_bugs]: { types: 'BUG' },
  [MetricKey.vulnerabilities]: { types: 'VULNERABILITY' },
  [MetricKey.new_vulnerabilities]: { types: 'VULNERABILITY' },
};

export function isIssueMeasure(metric: string) {
  return ISSUE_MEASURES.indexOf(metric as MetricKey) !== -1;
}

export function propsToIssueParams(metric: string, inNewCodePeriod = false) {
  const params: Dict<string | boolean> = {
    ...DEFAULT_ISSUES_QUERY,
    ...issueParamsPerMetric[metric],
  };

  if (inNewCodePeriod) {
    params.inNewCodePeriod = true;
  }

  return params;
}
