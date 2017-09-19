/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import ClusterSysInfos from '../ClusterSysInfos';
import { HealthType } from '../../../../api/system';
import { ClusterSysInfo } from '../../utils';

const sysInfoData: ClusterSysInfo = {
  System: {
    Health: HealthType.RED,
    'Health Causes': ['Database down'],
    'High Availability': true,
    'Logs Level': 'INFO'
  },
  'Application Nodes': [
    { Name: 'Bar', Health: HealthType.GREEN, 'Health Causes': [], 'Logs Level': 'INFO' }
  ],
  'Search Nodes': [
    { Name: 'Baz', Health: HealthType.YELLOW, 'Health Causes': [], 'Logs Level': 'INFO' }
  ]
};

it('should render correctly', () => {
  expect(
    getWrapper({
      sysInfoData: {
        ...sysInfoData,
        'Application Nodes': [
          { Name: 'Foo', Health: HealthType.GREEN, 'Health Causes': [], 'Logs Level': 'INFO' },
          { Name: 'Bar', Health: HealthType.RED, 'Health Causes': [], 'Logs Level': 'DEBUG' },
          { Name: 'Baz', Health: HealthType.YELLOW, 'Health Causes': [], 'Logs Level': 'TRACE' }
        ]
      }
    }).find('HealthCard')
  ).toHaveLength(5);
});

it('should support more than two nodes', () => {
  expect(getWrapper()).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <ClusterSysInfos
      expandedCards={['System', 'Foo']}
      sysInfoData={sysInfoData}
      toggleCard={() => {}}
      {...props}
    />
  );
}
