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
import { mockComponent, mockMeasure, mockShortLivingBranch } from '../../../../helpers/testMocks';
import MeasurementLabel from '../MeasurementLabel';

it('should render correctly for coverage', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      measures: [
        mockMeasure({ metric: 'new_coverage' }),
        mockMeasure({ metric: 'new_lines_to_cover' })
      ]
    })
  ).toMatchSnapshot();
});

it('should render correctly for duplications', () => {
  expect(
    shallowRender({
      measures: [mockMeasure({ metric: 'new_duplicated_lines_density' })],
      type: 'DUPLICATION'
    })
  ).toMatchSnapshot();
});

it('should render correctly with no value', () => {
  expect(shallowRender({ measures: [mockMeasure({ metric: 'NONE' })] })).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasurementLabel['props']> = {}) {
  return shallow(
    <MeasurementLabel
      branchLike={mockShortLivingBranch()}
      component={mockComponent()}
      measures={[mockMeasure({ metric: 'new_coverage' })]}
      type="COVERAGE"
      {...props}
    />
  );
}
