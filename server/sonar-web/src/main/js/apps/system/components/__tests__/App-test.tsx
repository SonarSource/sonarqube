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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getSystemInfo } from '../../../../api/system';
import {
  mockClusterSysInfo,
  mockLocation,
  mockRouter,
  mockStandaloneSysInfo,
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { App } from '../App';

jest.mock('../../../../api/system', () => ({
  getSystemInfo: jest.fn().mockResolvedValue(null),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');
  expect(
    wrapper.setState({ loading: false, sysInfoData: mockStandaloneSysInfo() })
  ).toMatchSnapshot('stand-alone sysinfo');
  expect(wrapper.setState({ loading: false, sysInfoData: mockClusterSysInfo() })).toMatchSnapshot(
    'cluster sysinfo'
  );
});

it('should fetch system info', async () => {
  const sysInfoData = mockStandaloneSysInfo();
  (getSystemInfo as jest.Mock).mockResolvedValue(sysInfoData);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getSystemInfo).toHaveBeenCalled();
  expect(wrapper.state().sysInfoData).toBe(sysInfoData);
});

it('should toggle cards and update the URL', () => {
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  // Open
  wrapper.instance().toggleSysInfoCards('foo');
  expect(replace).toHaveBeenCalledWith(
    expect.objectContaining({
      query: { expand: 'foo' },
    })
  );

  // Close.
  replace.mockClear();
  wrapper.setProps({ location: mockLocation({ query: { expand: 'foo,bar' } }) });
  wrapper.instance().toggleSysInfoCards('foo');
  expect(replace).toHaveBeenCalledWith(
    expect.objectContaining({
      query: { expand: 'bar' },
    })
  );
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(<App location={mockLocation()} router={mockRouter()} {...props} />);
}
