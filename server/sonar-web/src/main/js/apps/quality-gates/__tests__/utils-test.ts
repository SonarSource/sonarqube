/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { mockMetric } from '../../../helpers/testMocks';
import { getLocalizedMetricNameNoDiffMetric } from '../utils';

jest.mock('../../../store/rootReducer', () => ({
  getMetricByKey: (store: any, key: string) => store[key]
}));

jest.mock('../../../app/utils/getStore', () => ({
  default: () => ({
    getState: () => ({
      bugs: mockMetric({ key: 'bugs', name: 'Bugs' }),
      existing_metric: mockMetric(),
      new_maintainability_rating: mockMetric(),
      sqale_rating: mockMetric({ key: 'sqale_rating', name: 'Maintainability Rating' })
    })
  })
}));

describe('getLocalizedMetricNameNoDiffMetric', () => {
  it('should return the correct corresponding metric', () => {
    expect(getLocalizedMetricNameNoDiffMetric(mockMetric())).toBe('Coverage');
    expect(getLocalizedMetricNameNoDiffMetric(mockMetric({ key: 'new_bugs' }))).toBe('Bugs');
    expect(
      getLocalizedMetricNameNoDiffMetric(
        mockMetric({ key: 'new_custom_metric', name: 'Custom Metric on New Code' })
      )
    ).toBe('Custom Metric on New Code');
    expect(
      getLocalizedMetricNameNoDiffMetric(mockMetric({ key: 'new_maintainability_rating' }))
    ).toBe('Maintainability Rating');
  });
});
