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
  'overall_coverage': ['overall_uncovered_lines', 'overall_uncovered_conditions'],
  'overall_line_coverage': ['overall_uncovered_lines'],
  'overall_branch_coverage': ['overall_uncovered_conditions'],
  'overall_uncovered_lines': ['overall_line_coverage'],
  'overall_uncovered_conditions': ['overall_branch_coverage'],

  'new_overall_coverage': ['new_overall_uncovered_lines', 'new_overall_uncovered_conditions'],
  'new_overall_line_coverage': ['new_overall_uncovered_lines'],
  'new_overall_branch_coverage': ['new_overall_uncovered_conditions'],
  'new_overall_uncovered_lines': ['new_overall_line_coverage'],
  'new_overall_uncovered_conditions': ['new_overall_branch_coverage'],

  'coverage': ['uncovered_lines', 'uncovered_conditions'],
  'line_coverage': ['uncovered_lines'],
  'branch_coverage': ['uncovered_conditions'],
  'uncovered_lines': ['line_coverage'],
  'uncovered_conditions': ['branch_coverage'],

  'new_coverage': ['new_uncovered_lines', 'new_uncovered_conditions'],
  'new_line_coverage': ['new_uncovered_lines'],
  'new_branch_coverage': ['new_uncovered_conditions'],
  'new_uncovered_lines': ['new_line_coverage'],
  'new_uncovered_conditions': ['new_branch_coverage'],

  'it_coverage': ['it_uncovered_lines', 'it_uncovered_conditions'],
  'it_line_coverage': ['it_uncovered_lines'],
  'it_branch_coverage': ['it_uncovered_conditions'],
  'it_uncovered_lines': ['it_line_coverage'],
  'it_uncovered_conditions': ['it_branch_coverage'],

  'new_it_coverage': ['new_it_uncovered_lines', 'new_it_uncovered_conditions'],
  'new_it_line_coverage': ['new_it_uncovered_lines'],
  'new_it_branch_coverage': ['new_it_uncovered_conditions'],
  'new_it_uncovered_lines': ['new_it_line_coverage'],
  'new_it_uncovered_conditions': ['new_it_branch_coverage'],

  'duplicated_lines_density': ['duplicated_lines'],
  'duplicated_lines': ['duplicated_lines_density']
};
