/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockCondition, mockMetric } from '../../../helpers/testMocks';
import { MetricKey } from '../../../types/metrics';
import { Condition } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric, groupAndSortByPriorityConditions } from '../utils';

const METRICS = {
  existing_metric: mockMetric(),
  [MetricKey.new_maintainability_rating]: mockMetric({ name: 'New Maintainability Rating' }),
  [MetricKey.sqale_rating]: mockMetric({
    name: 'Maintainability Rating',
  }),
  [MetricKey.coverage]: mockMetric({ name: 'Coverage' }),
  [MetricKey.bugs]: mockMetric({ name: 'Bugs' }),
  [MetricKey.new_coverage]: mockMetric({ name: 'New Code Coverage' }),
  [MetricKey.new_reliability_rating]: mockMetric({ name: 'New Reliability Rating' }),
  [MetricKey.new_bugs]: mockMetric({ name: 'New Bugs' }),
  [MetricKey.code_smells]: mockMetric({ name: 'Code Smells' }),
  [MetricKey.duplicated_lines_density]: mockMetric({ name: 'Duplicated lines (%)' }),
};

describe('getLocalizedMetricNameNoDiffMetric', () => {
  it('should return the correct corresponding metric', () => {
    expect(getLocalizedMetricNameNoDiffMetric(mockMetric(), {})).toBe('coverage');
    expect(getLocalizedMetricNameNoDiffMetric(mockMetric({ key: 'new_bugs' }), METRICS)).toBe(
      'Bugs'
    );
    expect(
      getLocalizedMetricNameNoDiffMetric(
        mockMetric({ key: 'new_custom_metric', name: 'Custom Metric on New Code' }),
        METRICS
      )
    ).toBe('Custom Metric on New Code');
    expect(
      getLocalizedMetricNameNoDiffMetric(mockMetric({ key: 'new_maintainability_rating' }), METRICS)
    ).toBe('Maintainability Rating');
  });
});

describe('groupAndSortByPriorityConditions', () => {
  const conditions = [
    mockCondition(),
    mockCondition({ metric: MetricKey.bugs }),
    mockCondition({ metric: MetricKey.new_coverage }),
    mockCondition({ metric: MetricKey.new_reliability_rating }),
    mockCondition({ metric: MetricKey.code_smells }),
    mockCondition({ metric: MetricKey.duplicated_lines_density }),
    mockCondition({ metric: MetricKey.new_bugs }),
  ];
  const expectedConditionsOrderNewCode = [
    MetricKey.new_reliability_rating,
    MetricKey.new_coverage,
    MetricKey.new_bugs,
  ];
  const expectConditionsOrderOverallCode = [
    MetricKey.bugs,
    MetricKey.code_smells,
    MetricKey.coverage,
    MetricKey.duplicated_lines_density,
  ];

  it('should return grouped conditions by overall/new code and sort them by CAYC order', () => {
    const result = groupAndSortByPriorityConditions(conditions, METRICS);
    const conditionsMap = ({ metric }: Condition) => metric;

    expect(result.newCodeConditions.map(conditionsMap)).toEqual(expectedConditionsOrderNewCode);
    expect(result.overallCodeConditions.map(conditionsMap)).toEqual(
      expectConditionsOrderOverallCode
    );
  });
});
