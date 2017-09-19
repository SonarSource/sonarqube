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
import StandaloneSysInfos from '../StandaloneSysInfos';
import { HealthType } from '../../../../api/system';
import { StandaloneSysInfo } from '../../utils';

const sysInfoData: StandaloneSysInfo = {
  Cluster: true,
  Health: HealthType.RED,
  'Logs Level': 'DEBUG',
  Name: 'Foo',
  'Health Causes': [{ message: 'Database down' }],
  'Web JVM': { 'Max Memory': '2Gb' },
  'Compute Engine': { Pending: 4 },
  Elasticsearch: { 'Number of Nodes': 1 }
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
