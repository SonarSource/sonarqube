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
    expect(getLocalizedMetricNameNoDiffMetric(mockMetric(), {})).toBe(MetricKey.coverage);
    expect(
      getLocalizedMetricNameNoDiffMetric(mockMetric({ key: MetricKey.new_bugs }), METRICS),
    ).toBe('Bugs');
    expect(
      getLocalizedMetricNameNoDiffMetric(
        mockMetric({ key: 'new_custom_metric', name: 'Custom Metric on New Code' }),
        METRICS,
      ),
    ).toBe('Custom Metric on New Code');
    expect(
      getLocalizedMetricNameNoDiffMetric(
        mockMetric({ key: MetricKey.new_maintainability_rating }),
        METRICS,
      ),
    ).toBe('Maintainability Rating');
  });
});

describe('groupAndSortByPriorityConditions', () => {
  const conditions = [
    mockCondition(),
    mockCondition({ metric: MetricKey.new_duplicated_lines_density, isCaycCondition: true }),
    mockCondition({ metric: MetricKey.bugs }),
    mockCondition({ metric: MetricKey.new_coverage, isCaycCondition: true }),
    mockCondition({ metric: MetricKey.new_reliability_rating }),
    mockCondition({ metric: MetricKey.code_smells }),
    mockCondition({ metric: MetricKey.duplicated_lines_density }),
    mockCondition({ metric: MetricKey.new_violations, isCaycCondition: true }),
    mockCondition({ metric: MetricKey.new_bugs }),
    mockCondition({ metric: MetricKey.new_security_hotspots_reviewed, isCaycCondition: true }),
  ];
  const expectedConditionsOrderNewCode = [MetricKey.new_bugs, MetricKey.new_reliability_rating];
  const expectConditionsOrderOverallCode = [
    MetricKey.bugs,
    MetricKey.code_smells,
    MetricKey.coverage,
    MetricKey.duplicated_lines_density,
  ];
  const expectedConditionsOrderCayc = [
    MetricKey.new_violations,
    MetricKey.new_security_hotspots_reviewed,
    MetricKey.new_coverage,
    MetricKey.new_duplicated_lines_density,
  ];

  it('should return grouped conditions by overall/new code and sort them by CAYC order', () => {
    const result = groupAndSortByPriorityConditions(conditions, METRICS, true);
    const conditionsMap = ({ metric }: Condition) => metric;

    expect(result.newCodeConditions.map(conditionsMap)).toEqual(expectedConditionsOrderNewCode);
    expect(result.overallCodeConditions.map(conditionsMap)).toEqual(
      expectConditionsOrderOverallCode,
    );
    expect(result.caycConditions.map(conditionsMap)).toEqual(expectedConditionsOrderCayc);
  });

  it('should return grouped conditions and add CaYC conditions to new code if QG is not compliant', () => {
    const result = groupAndSortByPriorityConditions(conditions, METRICS, false);
    const conditionsMap = ({ metric }: Condition) => metric;

    expect(result.newCodeConditions.map(conditionsMap)).toEqual([
      MetricKey.new_violations,
      MetricKey.new_security_hotspots_reviewed,
      MetricKey.new_coverage,
      MetricKey.new_duplicated_lines_density,
      MetricKey.new_bugs,
      MetricKey.new_reliability_rating,
    ]);
    expect(result.overallCodeConditions.map(conditionsMap)).toEqual(
      expectConditionsOrderOverallCode,
    );
    expect(result.caycConditions.map(conditionsMap)).toEqual([]);
  });
});
