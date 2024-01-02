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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import { MeasurementType } from '../../utils';
import MeasurementLabel from '../MeasurementLabel';

it('should render correctly for coverage', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.lines_to_cover }) }),
      ],
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_lines_to_cover }) }),
      ],
      useDiffMetric: true,
    })
  ).toMatchSnapshot();
});

it('should render correctly for duplications', () => {
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.duplicated_lines_density }) }),
      ],
      type: MeasurementType.Duplication,
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.duplicated_lines_density }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.ncloc }) }),
      ],
      type: MeasurementType.Duplication,
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({
          metric: mockMetric({ key: MetricKey.new_duplicated_lines_density }),
        }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_lines }) }),
      ],
      type: MeasurementType.Duplication,
      useDiffMetric: true,
    })
  ).toMatchSnapshot();
});

it('should render correctly with no value', () => {
  expect(shallowRender({ measures: [] })).toMatchSnapshot();
});

it('should render correctly when centered', () => {
  expect(shallowRender({ centered: true })).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasurementLabel['props']> = {}) {
  return shallow(
    <MeasurementLabel
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) })]}
      type={MeasurementType.Coverage}
      {...props}
    />
  );
}
