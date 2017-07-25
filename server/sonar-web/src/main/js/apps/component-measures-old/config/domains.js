/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
export const domains = {
  Reliability: {
    main: ['bugs', 'new_bugs', 'reliability_rating'],
    order: [
      'bugs',
      'new_bugs',
      'reliability_rating',
      'reliability_remediation_effort',
      'new_reliability_remediation_effort'
    ]
  },

  Security: {
    main: ['vulnerabilities', 'new_vulnerabilities', 'security_rating'],
    order: [
      'vulnerabilities',
      'new_vulnerabilities',
      'security_rating',
      'security_remediation_effort',
      'new_security_remediation_effort'
    ]
  },

  Maintainability: {
    main: ['code_smells', 'new_code_smells', 'sqale_rating'],
    order: [
      'code_smells',
      'new_code_smells',
      'sqale_rating',
      'sqale_index',
      'new_technical_debt',
      'sqale_debt_ratio',
      'new_sqale_debt_ratio',
      'effort_to_reach_maintainability_rating_a'
    ]
  },

  Coverage: {
    main: ['coverage', 'new_coverage', 'tests'],
    order: [
      'coverage',
      'new_coverage',
      'line_coverage',
      'new_line_coverage',
      'branch_coverage',
      'new_branch_coverage',
      'uncovered_lines',
      'new_uncovered_lines',
      'uncovered_conditions',
      'new_uncovered_conditions',
      'new_lines_to_cover',

      'lines_to_cover',

      'tests',
      'test_success',
      'test_errors',
      'test_failures',
      'skipped_tests',
      'test_success_density',
      'test_execution_time'
    ]
  },

  Duplications: {
    main: ['duplicated_lines_density', 'new_duplicated_lines_density'],
    order: [
      'duplicated_lines_density',
      'new_duplicated_lines_density',
      'duplicated_blocks',
      'new_duplicated_blocks',
      'duplicated_lines',
      'new_duplicated_lines',
      'duplicated_files'
    ]
  },

  Size: {
    main: ['ncloc'],
    order: [
      'ncloc',
      'lines',
      'new_lines',
      'statements',
      'functions',
      'classes',
      'files',
      'directories'
    ]
  },

  Complexity: {
    main: ['complexity'],
    order: ['complexity', 'function_complexity', 'file_complexity', 'class_complexity']
  },

  Releasability: {
    main: ['alert_status', 'releasability_rating'],
    order: ['alert_status']
  },

  Issues: {
    main: ['violations', 'new_violations'],
    order: [
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
    ]
  }
};
