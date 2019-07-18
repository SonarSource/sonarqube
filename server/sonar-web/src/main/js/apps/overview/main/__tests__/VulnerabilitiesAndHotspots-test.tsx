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
import { ComposedProps } from '../enhance';
import VulnerabilitiesAndHotspots from '../VulnerabilitiesAndHotspots';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<ComposedProps> = {}) {
  return shallow(
    <VulnerabilitiesAndHotspots
      branchLike={mockMainBranch()}
      component={mockComponent()}
      history={{ vulnerabilities: [] }}
      leakPeriod={{ index: 1 } as T.Period}
      measures={[
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'vulnerabilities',
            key: 'vulnerabilities',
            name: 'Vulnerabilities',
            type: 'INT'
          }),
          value: '0'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'security_hotspots',
            key: 'security_hotspots',
            name: 'Security Hotspots',
            type: 'INT'
          }),
          value: '0'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'security_rating',
            key: 'security_rating',
            name: 'Security',
            type: 'RATING'
          }),
          value: '1.0'
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_vulnerabilities',
            key: 'new_vulnerabilities',
            name: 'New Vulnerabilities',
            type: 'INT'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '1'
            }
          ]
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_security_hotspots',
            key: 'new_security_hotspots',
            name: 'New Security Hotspots',
            type: 'INT'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '10'
            }
          ]
        }),
        mockMeasureEnhanced({
          metric: mockMetric({
            id: 'new_security_rating',
            key: 'new_security_rating',
            name: 'New Security Rating',
            type: 'RATING'
          }),
          periods: [
            {
              bestValue: true,
              index: 1,
              value: '5.0'
            }
          ]
        })
      ]}
      {...props}
    />
  ).dive();
}
