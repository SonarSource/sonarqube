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
import { mockClusterSysInfo } from '../../../../helpers/testMocks';
import ClusterSysInfos from '../ClusterSysInfos';

it('should render correctly', () => {
  expect(
    shallowRender(
      mockClusterSysInfo({
        sysInfoData: {
          Health: 'RED',
          'Health Causes': ['Database down'],
          'Application Nodes': [
            {
              Name: 'Foo',
              Health: 'GREEN',
              'Health Causes': [],
              'Compute Engine Logging': { 'Logs Level': 'INFO' },
              'Web Logging': { 'Logs Level': 'INFO' }
            },
            {
              Name: 'Bar',
              Health: 'RED',
              'Health Causes': [],
              'Compute Engine Logging': { 'Logs Level': 'INFO' },
              'Web Logging': { 'Logs Level': 'DEBUG' }
            },
            {
              Name: 'Baz',
              Health: 'YELLOW',
              'Health Causes': [],
              'Compute Engine Logging': { 'Logs Level': 'TRACE' },
              'Web Logging': { 'Logs Level': 'DEBUG' }
            }
          ]
        }
      })
    ).find('HealthCard')
  ).toHaveLength(4);
});

it('should support more than two nodes', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <ClusterSysInfos
      expandedCards={['System', 'Foo']}
      sysInfoData={mockClusterSysInfo({
        Health: 'RED',
        'Health Causes': ['Database down'],
        'Application Nodes': [
          {
            Name: 'Bar',
            Health: 'GREEN',
            'Health Causes': [],
            'Compute Engine Logging': { 'Logs Level': 'INFO' },
            'Web Logging': { 'Logs Level': 'INFO' }
          }
        ],
        'Search Nodes': [
          {
            Name: 'Baz',
            Health: 'YELLOW',
            'Health Causes': [],
            'Compute Engine Logging': { 'Logs Level': 'INFO' },
            'Web Logging': { 'Logs Level': 'INFO' }
          }
        ]
      })}
      toggleCard={() => {}}
      {...props}
    />
  );
}
