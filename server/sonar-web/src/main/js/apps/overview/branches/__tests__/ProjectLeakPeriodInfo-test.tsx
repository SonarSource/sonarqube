/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { differenceInDays } from 'date-fns';
import { shallow } from 'enzyme';
import * as React from 'react';
import { IntlShape } from 'react-intl';
import { mockPeriod } from '../../../../helpers/testMocks';
import { Period } from '../../../../types/types';
import { ProjectLeakPeriodInfo } from '../ProjectLeakPeriodInfo';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return { ...actual, differenceInDays: jest.fn().mockReturnValue(10) };
});

it('should render correctly for 10 days', () => {
  expect(shallowRender({ mode: 'days', parameter: '10' })).toMatchSnapshot();
});

it('should render correctly for a specific date', () => {
  expect(shallowRender({ mode: 'date', parameter: '2013-01-01' })).toMatchSnapshot();
});

it('should render correctly for a specific version', () => {
  expect(shallowRender({ mode: 'version', parameter: '0.1' })).toMatchSnapshot();
});

it('should render correctly for "previous_version"', () => {
  expect(shallowRender({ mode: 'previous_version' })).toMatchSnapshot();
});

it('should render correctly for "previous_analysis"', () => {
  expect(shallowRender({ mode: 'previous_analysis' })).toMatchSnapshot();
});

it('should render correctly for "REFERENCE_BRANCH"', () => {
  expect(shallowRender({ mode: 'REFERENCE_BRANCH', parameter: 'master' })).toMatchSnapshot();
});

it('should render correctly for "manual_baseline"', () => {
  expect(shallowRender({ mode: 'manual_baseline' })).toMatchSnapshot();
  expect(shallowRender({ mode: 'manual_baseline', parameter: '1.1.2' })).toMatchSnapshot();
});

it('should render a more precise date', () => {
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);
  expect(
    shallowRender({ date: '2018-08-17T00:00:00+0200', mode: 'previous_version' })
  ).toMatchSnapshot();
});

function shallowRender(period: Partial<Period> = {}) {
  return shallow(
    <ProjectLeakPeriodInfo
      intl={
        {
          formatDate: (date: string) => 'formatted.' + date,
          formatTime: (date: string) => 'formattedTime.' + date,
        } as IntlShape
      }
      leakPeriod={mockPeriod({ ...period })}
    />
  );
}
