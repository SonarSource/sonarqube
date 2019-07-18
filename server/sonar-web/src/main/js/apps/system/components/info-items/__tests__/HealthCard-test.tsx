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
import HealthCard from '../HealthCard';

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should show a main section and multiple sub sections', () => {
  const sysInfoData = {
    Name: 'foo',
    bar: 'Bar',
    Database: { db: 'test' },
    Elasticseach: { Elastic: 'search' }
  };
  expect(getWrapper({ open: true, sysInfoData })).toMatchSnapshot();
});

it('should display the log level alert', () => {
  expect(
    getWrapper({ sysInfoData: { 'Logs Level': 'DEBUG' } })
      .dive()
      .find('Alert')
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <HealthCard
      biggerHealth={false}
      health="RED"
      healthCauses={['foo']}
      name="Foobar"
      onClick={() => {}}
      open={false}
      sysInfoData={{}}
      {...props}
    />
  );
}
