/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { shallow } from 'enzyme';
import { keyBy } from 'lodash';
import * as React from 'react';
import { mockComponentMeasure, mockMeasure, mockMetric } from '../../../../helpers/testMocks';
import OriginalBubbleChart from '../../../../sonar-ui-common/components/charts/BubbleChart';
import { MetricKey } from '../../../../types/metrics';
import { enhanceComponent } from '../../utils';
import BubbleChart from '../BubbleChart';

const metrics = keyBy(
  [
    mockMetric({ key: MetricKey.ncloc, type: 'NUMBER' }),
    mockMetric({ key: MetricKey.security_remediation_effort, type: 'NUMBER' }),
    mockMetric({ key: MetricKey.vulnerabilities, type: 'NUMBER' }),
    mockMetric({ key: MetricKey.security_rating, type: 'RATING' })
  ],
  m => m.key
);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      components: [
        enhanceComponent(
          mockComponentMeasure(true, {
            measures: [
              mockMeasure({ value: '0', metric: MetricKey.ncloc }),
              mockMeasure({ value: '0', metric: MetricKey.security_remediation_effort }),
              mockMeasure({ value: '0', metric: MetricKey.vulnerabilities }),
              mockMeasure({ value: '0', metric: MetricKey.security_rating })
            ]
          }),
          metrics[MetricKey.vulnerabilities],
          metrics
        )
      ]
    })
  ).toMatchSnapshot('all on x=0');
});

it('should handle filtering', () => {
  const wrapper = shallowRender();
  expect(wrapper.find(OriginalBubbleChart).props().items).toHaveLength(1);

  wrapper.instance().handleRatingFilterClick(2);

  expect(wrapper.state().ratingFilters).toEqual({ 2: true });
  expect(wrapper.find(OriginalBubbleChart).props().items).toHaveLength(0);
});

function shallowRender(overrides: Partial<BubbleChart['props']> = {}) {
  return shallow<BubbleChart>(
    <BubbleChart
      component={mockComponentMeasure()}
      components={[
        enhanceComponent(
          mockComponentMeasure(true, {
            measures: [
              mockMeasure({ value: '236', metric: MetricKey.ncloc }),
              mockMeasure({ value: '10', metric: MetricKey.security_remediation_effort }),
              mockMeasure({ value: '3', metric: MetricKey.vulnerabilities }),
              mockMeasure({ value: '2', metric: MetricKey.security_rating })
            ]
          }),
          metrics[MetricKey.vulnerabilities],
          metrics
        )
      ]}
      domain="Security"
      metrics={metrics}
      updateSelected={jest.fn()}
      {...overrides}
    />
  );
}
