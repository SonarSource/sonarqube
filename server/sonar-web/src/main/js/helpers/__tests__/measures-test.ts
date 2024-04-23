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
import { MetricKey } from '../../types/metrics';
import { CCT_SOFTWARE_QUALITY_METRICS } from '../constants';
import {
  areCCTMeasuresComputed,
  enhanceConditionWithMeasure,
  isPeriodBestValue,
} from '../measures';
import { mockQualityGateStatusCondition } from '../mocks/quality-gates';
import { mockMeasure, mockMeasureEnhanced, mockMetric } from '../testMocks';

describe('enhanceConditionWithMeasure', () => {
  it('should correctly map enhance conditions with measure data', () => {
    const measures = [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }), period: undefined }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) }),
    ];

    expect(
      enhanceConditionWithMeasure(
        mockQualityGateStatusCondition({ metric: MetricKey.bugs }),
        measures,
      ),
    ).toMatchObject({
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.bugs }),
      }),
    });

    expect(
      enhanceConditionWithMeasure(
        mockQualityGateStatusCondition({ metric: MetricKey.new_bugs }),
        measures,
      ),
    ).toMatchObject({
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.new_bugs }),
      }),
      period: 1,
    });
  });

  it('should return undefined if no match can be found', () => {
    expect(enhanceConditionWithMeasure(mockQualityGateStatusCondition(), [])).toBeUndefined();
  });
});

describe('isPeriodBestValue', () => {
  it('should work as expected', () => {
    expect(isPeriodBestValue(mockMeasureEnhanced({ period: undefined }))).toBe(false);
    expect(
      isPeriodBestValue(
        mockMeasureEnhanced({ period: { index: 1, value: '1.0', bestValue: false } }),
      ),
    ).toBe(false);
    expect(
      isPeriodBestValue(
        mockMeasureEnhanced({ period: { index: 1, value: '1.0', bestValue: true } }),
      ),
    ).toBe(true);
  });
});

describe('areCCTMeasuresComputed', () => {
  it('returns true when measures include maintainability_,security_,reliability_issues', () => {
    expect(
      areCCTMeasuresComputed(CCT_SOFTWARE_QUALITY_METRICS.map((metric) => mockMeasure({ metric }))),
    ).toBe(true);
  });

  it('returns false otherwise', () => {
    expect(areCCTMeasuresComputed([mockMeasure()])).toBe(false);
    expect(
      areCCTMeasuresComputed([
        mockMeasure(),
        mockMeasure({ metric: CCT_SOFTWARE_QUALITY_METRICS[0] }),
      ]),
    ).toBe(false);
  });
});
