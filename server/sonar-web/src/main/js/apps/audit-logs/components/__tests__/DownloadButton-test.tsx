/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { subDays } from 'date-fns';
import { shallow } from 'enzyme';
import * as React from 'react';
import { RangeOption } from '../../utils';
import DownloadButton, { DownloadButtonProps } from '../DownloadButton';

jest.mock('date-fns', () => {
  const { subDays } = jest.requireActual('date-fns');
  return {
    endOfDay: jest.fn().mockImplementation(d => d),
    startOfDay: jest.fn().mockImplementation(d => d),
    subDays
  };
});

jest.mock('../../utils', () => {
  const { HousekeepingPolicy, RangeOption } = jest.requireActual('../../utils');
  const now = new Date('2020-07-21T12:00:00Z');

  return {
    HousekeepingPolicy,
    now: jest.fn().mockReturnValue(now),
    RangeOption
  };
});

it.each([[RangeOption.Today], [RangeOption.Week], [RangeOption.Month], [RangeOption.Trimester]])(
  'should render correctly for %s',
  selection => {
    expect(shallowRender({ selection })).toMatchSnapshot('default');
  }
);

it('should render correctly for custom range', () => {
  const baseDate = new Date('2020-07-21T12:00:00Z');

  expect(shallowRender({ selection: RangeOption.Custom })).toMatchSnapshot('no dates');
  expect(
    shallowRender({
      dateRange: { from: subDays(baseDate, 2), to: baseDate },
      selection: RangeOption.Custom
    })
  ).toMatchSnapshot('with dates');
});

it('should handle download', () => {
  const onStartDownload = jest.fn();
  const wrapper = shallowRender({ onStartDownload });

  wrapper.find('a').simulate('click');
  wrapper.setProps({ downloadStarted: true });
  wrapper.find('a').simulate('click');

  expect(onStartDownload).toBeCalledTimes(1);
});

function shallowRender(props: Partial<DownloadButtonProps> = {}) {
  return shallow<DownloadButtonProps>(
    <DownloadButton
      downloadStarted={false}
      selection={RangeOption.Today}
      onStartDownload={jest.fn()}
      {...props}
    />
  );
}
