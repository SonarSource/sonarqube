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
// @flow
export const bubbles = {
  Reliability: {
    x: 'ncloc',
    y: 'reliability_remediation_effort',
    size: 'bugs',
    colors: ['reliability_rating']
  },
  Security: {
    x: 'ncloc',
    y: 'security_remediation_effort',
    size: 'vulnerabilities',
    colors: ['security_rating']
  },
  Maintainability: {
    x: 'ncloc',
    y: 'sqale_index',
    size: 'code_smells',
    colors: ['sqale_rating']
  },
  Coverage: { x: 'complexity', y: 'coverage', size: 'uncovered_lines', yDomain: [100, 0] },
  Duplications: { x: 'ncloc', y: 'duplicated_lines', size: 'duplicated_blocks' },
  project_overview: {
    x: 'sqale_index',
    y: 'coverage',
    size: 'ncloc',
    colors: ['reliability_rating', 'security_rating'],
    yDomain: [100, 0]
  }
};
