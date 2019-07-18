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
import CodeSmells from '../CodeSmells';
import { ComposedProps } from '../enhance';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<ComposedProps> = {}) {
  return shallow(
    <CodeSmells
      branchLike={mockMainBranch()}
      component={mockComponent()}
      history={{ sqale_index: [] }}
      leakPeriod={{ index: 1 } as T.Period}
      measures={[
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'code_smells',
            key: 'code_smells',
            name: 'Code Smells',
            type: 'INT'
          }),
          value: '15'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'sqale_index',
            key: 'sqale_index',
            name: 'Debt',
            type: 'INT'
          }),
          value: '1052'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'sqale_rating',
            key: 'sqale_rating',
            name: 'Maintainability',
            type: 'RATING'
          }),
          value: '2.0'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_code_smells',
            key: 'new_code_smells',
            name: 'New Code Smells',
            type: 'INT'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '52'
            }
          ]
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_technical_debt',
            key: 'new_technical_debt',
            name: 'New Debt',
            type: 'INT'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '85'
            }
          ]
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_maintainability_rating',
            key: 'new_maintainability_rating',
            name: 'New Maintainability',
            type: 'RATING'
          }),
          value: '3.0'
        })
      ]}
      {...props}
    />
  ).dive();
}
