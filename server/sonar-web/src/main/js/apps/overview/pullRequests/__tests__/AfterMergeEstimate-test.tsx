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
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import { MeasurementType } from '../../utils';
import { AfterMergeEstimate, AfterMergeEstimateProps } from '../AfterMergeEstimate';

it('should render correctly for coverage', () => {
  expect(shallowRender()).toMatchSnapshot();
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
});

it('should render correctly with no value', () => {
  expect(shallowRender({ measures: [] })).toMatchSnapshot();
});

function shallowRender(props: Partial<AfterMergeEstimateProps> = {}) {
  return shallow(
    <AfterMergeEstimate
      measures={[mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) })]}
      type={MeasurementType.Coverage}
      {...props}
    />
  );
}
