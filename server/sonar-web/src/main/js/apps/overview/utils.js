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
//@flow

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

export const HISTORY_METRICS_LIST = [
  'sqale_index',
  'duplicated_lines_density',
  'ncloc',
  'coverage'
];
