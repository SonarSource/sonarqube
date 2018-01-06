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
import React from 'react';
import { shallow } from 'enzyme';
import LeakPeriodLegend from '../LeakPeriodLegend';

const PROJECT = {
  key: 'foo',
  qualifier: 'TRK'
};

const APP = {
  key: 'bar',
  qualifier: 'APP'
};

const PERIOD = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'previous_version',
  parameter: '6,4'
};

const PERIOD_DAYS = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'days',
  parameter: '18'
};

it('should render correctly', () => {
  expect(shallow(<LeakPeriodLegend component={PROJECT} period={PERIOD} />)).toMatchSnapshot();
  expect(shallow(<LeakPeriodLegend component={PROJECT} period={PERIOD_DAYS} />)).toMatchSnapshot();
});

it('should render correctly for APP', () => {
  expect(shallow(<LeakPeriodLegend component={APP} period={PERIOD} />)).toMatchSnapshot();
});
