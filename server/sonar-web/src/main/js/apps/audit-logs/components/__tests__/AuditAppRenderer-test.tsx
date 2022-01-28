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
import { shallow } from 'enzyme';
import * as React from 'react';
import { HousekeepingPolicy, RangeOption } from '../../utils';
import AuditAppRenderer, { AuditAppRendererProps } from '../AuditAppRenderer';

jest.mock('../../utils', () => {
  const { HousekeepingPolicy, RangeOption } = jest.requireActual('../../utils');
  const now = new Date('2020-07-21T12:00:00Z');

  return {
    HousekeepingPolicy,
    now: jest.fn().mockReturnValue(now),
    RangeOption
  };
});

it.each([
  [HousekeepingPolicy.Weekly],
  [HousekeepingPolicy.Monthly],
  [HousekeepingPolicy.Trimestrial],
  [HousekeepingPolicy.Yearly]
])('should render correctly for %s housekeeping policy', housekeepingPolicy => {
  expect(shallowRender({ housekeepingPolicy })).toMatchSnapshot();
});

function shallowRender(props: Partial<AuditAppRendererProps> = {}) {
  return shallow(
    <AuditAppRenderer
      downloadStarted={false}
      handleDateSelection={jest.fn()}
      handleOptionSelection={jest.fn()}
      handleStartDownload={jest.fn()}
      housekeepingPolicy={HousekeepingPolicy.Yearly}
      selection={RangeOption.Today}
      {...props}
    />
  );
}
