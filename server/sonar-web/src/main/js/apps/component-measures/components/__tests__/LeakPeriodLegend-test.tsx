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
import * as React from 'react';
import { shallow } from 'enzyme';
import { InjectedIntlProps } from 'react-intl';
import { LeakPeriodLegend } from '../LeakPeriodLegend';
import { differenceInDays } from '../../../../helpers/dates';

jest.mock('../../../../helpers/dates', () => {
  const dates = require.requireActual('../../../../helpers/dates');
  dates.differenceInDays = jest.fn().mockReturnValue(10);
  return dates;
});

const PROJECT = {
  key: 'foo',
  name: 'Foo',
  qualifier: 'TRK'
};

const APP = {
  key: 'bar',
  name: 'Bar',
  qualifier: 'APP'
};

const PERIOD: T.Period = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'previous_version',
  parameter: '6,4'
};

const PERIOD_DAYS: T.Period = {
  date: '2017-05-16T13:50:02+0200',
  index: 1,
  mode: 'days',
  parameter: '18'
};

it('should render correctly', () => {
  expect(getWrapper(PROJECT, PERIOD)).toMatchSnapshot();
  expect(getWrapper(PROJECT, PERIOD_DAYS)).toMatchSnapshot();
});

it('should render correctly for APP', () => {
  expect(getWrapper(APP, PERIOD)).toMatchSnapshot();
});

it('should render a more precise date', () => {
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);
  expect(getWrapper(PROJECT, PERIOD)).toMatchSnapshot();
});

function getWrapper(component: T.ComponentMeasure, period: T.Period) {
  return shallow(
    <LeakPeriodLegend
      component={component}
      intl={{ formatDate: (x: any) => x } as InjectedIntlProps['intl']}
      period={period}
    />
  );
}
