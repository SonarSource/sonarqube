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
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AdminPageExtension } from '../../../../types/extension';
import { HousekeepingPolicy, RangeOption } from '../../utils';
import { AuditApp } from '../AuditApp';
import AuditAppRenderer from '../AuditAppRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should do nothing if governance is not available', async () => {
  const fetchValues = jest.fn();
  const wrapper = shallowRender({ fetchValues, adminPages: [] });
  await waitAndUpdate(wrapper);

  expect(wrapper.type()).toBeNull();
  expect(fetchValues).not.toBeCalled();
});

it('should fetch houskeeping policy on mount', async () => {
  const fetchValues = jest.fn();
  const wrapper = shallowRender({ fetchValues });
  await waitAndUpdate(wrapper);
  expect(fetchValues).toBeCalled();
});

it('should handle date selection', () => {
  const wrapper = shallowRender();
  const range = { from: subDays(new Date(), 2), to: new Date() };

  expect(wrapper.state().selection).toBe(RangeOption.Today);

  wrapper
    .find(AuditAppRenderer)
    .props()
    .handleDateSelection(range);

  expect(wrapper.state().selection).toBe(RangeOption.Custom);
  expect(wrapper.state().dateRange).toBe(range);
});

it('should handle predefined selection', () => {
  const wrapper = shallowRender();
  const dateRange = { from: subDays(new Date(), 2), to: new Date() };

  wrapper.setState({ dateRange, selection: RangeOption.Custom });

  wrapper
    .find(AuditAppRenderer)
    .props()
    .handleOptionSelection(RangeOption.Week);

  expect(wrapper.state().selection).toBe(RangeOption.Week);
  expect(wrapper.state().dateRange).toBeUndefined();
});

function shallowRender(props: Partial<AuditApp['props']> = {}) {
  return shallow<AuditApp>(
    <AuditApp
      auditHousekeepingPolicy={HousekeepingPolicy.Monthly}
      fetchValues={jest.fn()}
      adminPages={[{ key: AdminPageExtension.GovernanceConsole, name: 'name' }]}
      {...props}
    />
  );
}
