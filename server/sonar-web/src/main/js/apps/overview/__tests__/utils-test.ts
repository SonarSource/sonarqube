/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
  mockMeasureEnhanced,
  mockMetric,
  mockQualityGateStatusCondition
} from '../../../helpers/testMocks';
import { getThreshold } from '../utils';

// eslint-disable-next-line no-console
console.error = jest.fn();

describe('getThreshold', () => {
  it('return undefined if condition is not found', () => {
    expect(getThreshold([], '')).toBeUndefined();
    expect(getThreshold([mockMeasure()], '')).toBeUndefined();
    expect(
      getThreshold(
        [
          {
            metric: mockMetric({ key: 'quality_gate_details' }),
            value: 'badly typed json should fail'
          }
        ],
        ''
      )
    ).toBeUndefined();
    // eslint-disable-next-line no-console
    expect(console.error).toBeCalled();
  });

  it('should return the threshold for the right metric', () => {
    expect(getThreshold([mockMeasure()], 'new_coverage')).toBe(85);
    expect(getThreshold([mockMeasure()], 'new_duplicated_lines_density')).toBe(5);
  });
});

function mockMeasure() {
  return mockMeasureEnhanced({
    metric: mockMetric({ key: 'quality_gate_details' }),
    value: JSON.stringify({
      conditions: [
        mockQualityGateStatusCondition({
          metric: 'new_coverage',
          level: 'ERROR',
          error: '85'
        }),
        mockQualityGateStatusCondition({
          metric: 'new_duplicated_lines_density',
          level: 'WARNING',
          warning: '5'
        })
      ]
    })
  });
}
