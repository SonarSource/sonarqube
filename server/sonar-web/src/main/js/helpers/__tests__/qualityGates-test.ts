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

import {
  mockQualityGateApplicationStatus,
  mockQualityGateProjectStatus,
} from '../mocks/quality-gates';
import {
  extractStatusConditionsFromApplicationStatusChildProject,
  extractStatusConditionsFromProjectStatus,
} from '../qualityGates';

describe('extractStatusConditionsFromProjectStatus', () => {
  it('should correclty extract the conditions for the project status', () => {
    expect(extractStatusConditionsFromProjectStatus(mockQualityGateProjectStatus())).toEqual([
      {
        actual: '0',
        error: '1.0',
        level: 'OK',
        metric: 'new_bugs',
        op: 'GT',
        period: 1,
      },
    ]);
  });
});

describe('extractStatusConditionsFromApplicationStatusChildProject', () => {
  it('should correclty extract the conditions for the application child project status', () => {
    expect(
      extractStatusConditionsFromApplicationStatusChildProject(
        mockQualityGateApplicationStatus().projects[0],
      ),
    ).toEqual([
      {
        actual: '10',
        error: '1.0',
        level: 'ERROR',
        metric: 'coverage',
        op: 'GT',
        period: undefined,
      },
      {
        actual: '5',
        error: '1.0',
        level: 'ERROR',
        metric: 'new_bugs',
        op: 'GT',
        period: 1,
      },
    ]);
  });
});
