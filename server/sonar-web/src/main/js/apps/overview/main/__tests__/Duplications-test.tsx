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
import {
  mockComponent,
  mockMainBranch,
  mockMeasureEnhanced,
  mockMetric
} from '../../../../helpers/testMocks';
import Duplications from '../Duplications';
import { ComposedProps } from '../enhance';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<ComposedProps> = {}) {
  return shallow(
    <Duplications
      branchLike={mockMainBranch()}
      component={mockComponent()}
      leakPeriod={{ index: 1 } as T.Period}
      measures={[
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'duplicated_lines_density',
            key: 'duplicated_lines_density',
            name: 'Duplicated Lines'
          }),
          value: '0.5'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_lines',
            key: 'new_lines',
            name: 'New Lines',
            type: 'INT'
          }),
          value: '52'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_duplicated_lines_density',
            key: 'new_duplicated_lines_density',
            name: 'New Duplicated Lines'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '1.5'
            }
          ]
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'duplicated_blocks',
            key: 'duplicated_blocks',
            name: 'Duplicated Blocks',
            type: 'INT'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '2'
            }
          ]
        })
      ]}
      {...props}
    />
  ).dive();
}
