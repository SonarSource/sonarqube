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
import HealthCard from '../HealthCard';
import { click } from '../../../../../helpers/testUtils';
import { HealthType } from '../../../../../api/system';

it('should render correctly', () => {
  expect(getShallowWrapper()).toMatchSnapshot();
});

it('should display the sysinfo detail', () => {
  expect(getShallowWrapper({ biggerHealth: true, open: true })).toMatchSnapshot();
});

it('should show the sysinfo detail when the card is clicked', () => {
  const onClick = jest.fn();
  click(getShallowWrapper({ onClick }).find('.boxed-group-header'));
  expect(onClick).toBeCalled();
  expect(onClick).toBeCalledWith('Foobar');
});

it('should show a main section and multiple sub sections', () => {
  const sysInfoData = {
    Name: 'foo',
    bar: 'Bar',
    Database: { db: 'test' },
    Elasticseach: { Elastic: 'search' }
  };
  expect(getShallowWrapper({ open: true, sysInfoData })).toMatchSnapshot();
});

it('should display the log level alert', () => {
  expect(
    getShallowWrapper({ sysInfoData: { 'Logs Level': 'DEBUG' } }).find('.alert')
  ).toMatchSnapshot();
});

function getShallowWrapper(props = {}) {
  return shallow(
    <HealthCard
      biggerHealth={false}
      health={HealthType.RED}
      healthCauses={['foo']}
      name="Foobar"
      onClick={() => {}}
      open={false}
      sysInfoData={{}}
      {...props}
    />
  );
}
