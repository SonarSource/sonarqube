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
import Bugs from '../Bugs';
import { ComposedProps } from '../enhance';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<ComposedProps> = {}) {
  return shallow(
    <Bugs
      branchLike={mockMainBranch()}
      component={mockComponent()}
      history={{ bugs: [] }}
      historyStartDate={new Date('2019-01-14T15:44:51.000Z')}
      leakPeriod={{ index: 1, mode: 'days' } as T.Period}
      measures={[
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'bugs',
            key: 'bugs',
            name: 'Bugs',
            type: 'INT'
          }),
          value: '5'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_bugs',
            key: 'new_bugs',
            name: 'New Bugs',
            type: 'INT'
          }),
          value: '2'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'reliability_rating',
            key: 'reliability_rating',
            name: 'Reliability',
            type: 'RATING'
          }),
          value: '1.0'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_reliability_rating',
            key: 'new_reliability_rating',
            name: 'New Reliability',
            type: 'RATING'
          }),
          value: '2.0'
        })
      ]}
      {...props}
    />
  ).dive();
}
