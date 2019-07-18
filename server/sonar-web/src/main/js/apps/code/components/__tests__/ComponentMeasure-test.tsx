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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponentMeasure, mockMeasure, mockMetric } from '../../../../helpers/testMocks';
import ComponentMeasure from '../ComponentMeasure';

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders correctly for leak values', () => {
  expect(
    shallow(
      <ComponentMeasure
        component={mockComponentMeasure(false, {
          measures: [mockMeasure({ metric: 'new_coverage' })]
        })}
        metric={mockMetric({ key: 'new_coverage', name: 'Coverage on New Code' })}
      />
    )
  ).toMatchSnapshot();
});

it('renders correctly when component has no measures', () => {
  expect(
    shallowRender({ component: mockComponentMeasure(false, { measures: undefined }) })
  ).toMatchSnapshot();
});

it('should render correctly when no measure matches the metric', () => {
  expect(shallowRender({ metric: mockMetric({ key: 'nonexistent_key' }) })).toMatchSnapshot();
});

it('should render correctly for releasability rating', () => {
  expect(
    shallowRender({
      component: mockComponentMeasure(false, {
        measures: [mockMeasure({ metric: 'alert_status' })]
      }),
      metric: mockMetric({ key: 'releasability_rating' })
    })
  ).toMatchSnapshot();
});

function shallowRender(overrides: Partial<ComponentMeasure['props']> = {}) {
  return shallow(
    <ComponentMeasure
      component={mockComponentMeasure(false, { measures: [mockMeasure({ metric: 'coverage' })] })}
      metric={mockMetric()}
      {...overrides}
    />
  );
}
