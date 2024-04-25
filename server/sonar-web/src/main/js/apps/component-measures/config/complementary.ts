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
import { MetricKey } from '~sonar-aligned/types/metrics';
import { Dict } from '../../../types/types';

export const complementary: Dict<MetricKey[]> = {
  coverage: [MetricKey.uncovered_lines, MetricKey.uncovered_conditions],
  line_coverage: [MetricKey.uncovered_lines],
  branch_coverage: [MetricKey.uncovered_conditions],
  uncovered_lines: [MetricKey.line_coverage],
  uncovered_conditions: [MetricKey.branch_coverage],

  new_coverage: [MetricKey.new_uncovered_lines, MetricKey.new_uncovered_conditions],
  new_line_coverage: [MetricKey.new_uncovered_lines],
  new_branch_coverage: [MetricKey.new_uncovered_conditions],
  new_uncovered_lines: [MetricKey.new_line_coverage],
  new_uncovered_conditions: [MetricKey.new_branch_coverage],

  duplicated_lines_density: [MetricKey.duplicated_lines],
  new_duplicated_lines_density: [MetricKey.new_duplicated_lines],
  duplicated_lines: [MetricKey.duplicated_lines_density],
  new_duplicated_lines: [MetricKey.new_duplicated_lines_density],
};
