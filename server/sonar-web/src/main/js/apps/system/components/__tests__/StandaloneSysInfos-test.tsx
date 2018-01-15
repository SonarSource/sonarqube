/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import StandaloneSysInfos from '../StandaloneSysInfos';
import { HealthType, SysInfo } from '../../../../api/system';

const sysInfoData: SysInfo = {
  Health: HealthType.RED,
  'Health Causes': ['Database down'],
  'Web JVM': { 'Max Memory': '2Gb' },
  'Compute Engine': { Pending: 4 },
  Search: { 'Number of Nodes': 1 },
  System: {
    'High Availability': true,
    'Logs Level': 'DEBUG',
    'Server ID': 'MyServerId'
  }
};

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <StandaloneSysInfos
      expandedCards={['Compute Engine', 'Foo']}
      sysInfoData={sysInfoData}
      toggleCard={() => {}}
      {...props}
    />
  );
}
