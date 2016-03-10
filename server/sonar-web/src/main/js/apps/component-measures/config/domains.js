/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
export default {
  'Issues': [
    'violations',
    'new_violations',
    'blocker_violations',
    'new_blocker_violations',
    'critical_violations',
    'new_critical_violations',
    'major_violations',
    'new_major_violations',
    'minor_violations',
    'new_minor_violations',
    'info_violations',
    'new_info_violations',
    'open_issues',
    'reopened_issues',
    'confirmed_issues',
    'false_positive_issues'
  ],

  'Maintainability': [
    'code_smells',
    'new_code_smells',
    'sqale_index',
    'new_technical_debt',
    'sqale_rating',
    'sqale_debt_ratio',
    'new_sqale_debt_ratio',
    'effort_to_reach_maintainability_rating_a'
  ],

  'Reliability': [
    'bugs',
    'new_bugs',
    'reliability_rating',
    'reliability_remediation_effort',
    'new_reliability_remediation_effort',
    'effort_to_reach_reliability_rating_a'
  ],

  'Security': [
    'vulnerabilities',
    'new_vulnerabilities',
    'security_rating',
    'security_remediation_effort',
    'new_security_remediation_effort',
    'effort_to_reach_security_rating_a'
  ],

  'Size': [
    'ncloc',
    'lines'
  ],

  'Tests': [
    'overall_coverage',
    'overall_line_coverage',
    'overall_branch_coverage',
    'overall_uncovered_lines',
    'overall_uncovered_conditions',
    'coverage',
    'line_coverage',
    'branch_coverage',
    'uncovered_lines',
    'uncovered_conditions',
    'it_coverage',
    'it_line_coverage',
    'it_branch_coverage',
    'it_uncovered_lines',
    'it_uncovered_conditions',
    'lines_to_cover',
    'tests',
    'test_success',
    'test_errors',
    'test_failures',
    'skipped_tests',
    'test_success_density',
    'test_execution_time'
  ]
};
