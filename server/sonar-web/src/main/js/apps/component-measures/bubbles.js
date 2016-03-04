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
const bubblesConfig = {
  'code_smells': { x: 'ncloc', y: 'sqale_index', size: 'code_smells' },
  'sqale_index': { x: 'ncloc', y: 'code_smells', size: 'sqale_index' },

  'coverage': { x: 'complexity', y: 'coverage', size: 'uncovered_lines' },
  'it_coverage': { x: 'complexity', y: 'it_coverage', size: 'it_uncovered_lines' },
  'overall_coverage': { x: 'complexity', y: 'overall_coverage', size: 'overall_uncovered_lines' },

  'uncovered_lines': { x: 'complexity', y: 'coverage', size: 'uncovered_lines' },
  'it_uncovered_lines': { x: 'complexity', y: 'it_coverage', size: 'it_uncovered_lines' },
  'overall_uncovered_lines': { x: 'complexity', y: 'overall_coverage', size: 'overall_uncovered_lines' },

  'uncovered_conditions': { x: 'complexity', y: 'coverage', size: 'uncovered_conditions' },
  'it_uncovered_conditions': { x: 'complexity', y: 'it_coverage', size: 'it_uncovered_conditions' },
  'overall_uncovered_conditions': { x: 'complexity', y: 'overall_coverage', size: 'overall_uncovered_conditions' },

  'duplicated_lines': { x: 'ncloc', y: 'duplicated_lines', size: 'duplicated_blocks' },
  'duplicated_blocks': { x: 'ncloc', y: 'duplicated_lines', size: 'duplicated_blocks' }
};

export default bubblesConfig;
