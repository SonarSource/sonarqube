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
import React from 'react';
import { shallow } from 'enzyme';
import { UnconnectedApp } from '../App';
import OverviewApp from '../OverviewApp';
import EmptyOverview from '../EmptyOverview';

it('should render OverviewApp', () => {
  const component = { id: 'id', analysisDate: '2016-01-01' };
  const output = shallow(<UnconnectedApp component={component} />);
  expect(output.type()).toBe(OverviewApp);
});

it('should render EmptyOverview', () => {
  const component = { id: 'id' };
  const output = shallow(<UnconnectedApp component={component} />);
  expect(output.type()).toBe(EmptyOverview);
});

it('should pass leakPeriodIndex', () => {
  const component = { id: 'id', analysisDate: '2016-01-01' };
  const output = shallow(<UnconnectedApp component={component} />);
  expect(output.prop('leakPeriodIndex')).toBe('1');
});
