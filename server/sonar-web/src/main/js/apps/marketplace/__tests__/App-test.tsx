/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  getAvailablePlugins,
  getInstalledPlugins,
  getInstalledPluginsWithUpdates,
  getPluginUpdates
} from '../../../api/plugins';
import { mockLocation, mockRouter } from '../../../helpers/testMocks';
import { App } from '../App';

jest.mock('../../../api/plugins', () => ({
  getAvailablePlugins: jest.fn().mockResolvedValue({ plugins: [] }),
  getInstalledPlugins: jest.fn().mockResolvedValue([]),
  getInstalledPluginsWithUpdates: jest.fn().mockResolvedValue([]),
  getPluginUpdates: jest.fn().mockResolvedValue([])
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');
  expect(wrapper.setState({ loadingPlugins: false })).toMatchSnapshot('loaded');
});

it('should fetch plugin info', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getInstalledPluginsWithUpdates).toBeCalled();
  expect(getAvailablePlugins).toBeCalled();

  wrapper.setProps({ location: mockLocation({ query: { filter: 'updates' } }) });
  await waitAndUpdate(wrapper);
  expect(getPluginUpdates).toBeCalled();

  wrapper.setProps({ location: mockLocation({ query: { filter: 'installed' } }) });
  await waitAndUpdate(wrapper);
  expect(getInstalledPlugins).toBeCalled();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      fetchPendingPlugins={jest.fn()}
      location={mockLocation()}
      pendingPlugins={{
        installing: [],
        updating: [],
        removing: []
      }}
      router={mockRouter()}
      updateCenterActive={false}
      {...props}
    />
  );
}
