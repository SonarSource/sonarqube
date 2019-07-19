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

import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { Notifications } from '../Notifications';

jest.mock('../../../../api/notifications', () => ({
  addNotification: jest.fn(() => Promise.resolve()),
  getNotifications: jest.fn(() =>
    Promise.resolve({
      channels: ['channel1', 'channel2'],
      globalTypes: ['type-global', 'type-common'],
      notifications: [
        { channel: 'channel1', type: 'type-global' },
        { channel: 'channel1', type: 'type-common' },
        {
          channel: 'channel2',
          type: 'type-common',
          project: 'foo',
          projectName: 'Foo',
          organization: 'org'
        }
      ],
      perProjectTypes: ['type-common']
    })
  ),
  removeNotification: jest.fn(() => Promise.resolve())
}));

const api = require('../../../../api/notifications');

const addNotification = api.addNotification as jest.Mock<any>;
const getNotifications = api.getNotifications as jest.Mock<any>;
const removeNotification = api.removeNotification as jest.Mock<any>;

beforeEach(() => {
  addNotification.mockClear();
  getNotifications.mockClear();
  removeNotification.mockClear();
});

it('should fetch notifications and render', async () => {
  const wrapper = await shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(getNotifications).toBeCalled();
});

it('should add global notification', async () => {
  const notification = { channel: 'channel2', type: 'type-global' };
  const wrapper = await shallowRender();
  wrapper.find('GlobalNotifications').prop<Function>('addNotification')(notification);
  // `state` must be immediately updated
  expect(wrapper.state('notifications')).toContainEqual(notification);
  expect(addNotification).toBeCalledWith(notification);
});

it('should remove project notification', async () => {
  const notification = { channel: 'channel2', project: 'foo', type: 'type-common' };
  const wrapper = await shallowRender();
  expect(wrapper.state('notifications')).toContainEqual({
    ...notification,
    organization: 'org',
    projectName: 'Foo'
  });
  wrapper.find('Projects').prop<Function>('removeNotification')(notification);
  // `state` must be immediately updated
  expect(wrapper.state('notifications')).not.toContainEqual(notification);
  expect(removeNotification).toBeCalledWith(notification);
});

it('should NOT fetch organizations', async () => {
  const fetchOrganizations = jest.fn();
  await shallowRender({ fetchOrganizations });
  expect(getNotifications).toBeCalled();
  expect(fetchOrganizations).not.toBeCalled();
});

it('should fetch organizations', async () => {
  const fetchOrganizations = jest.fn();
  await shallowRender({ appState: { organizationsEnabled: true }, fetchOrganizations });
  expect(getNotifications).toBeCalled();
  expect(fetchOrganizations).toBeCalledWith(['org']);
});

async function shallowRender(props?: Partial<Notifications['props']>) {
  const wrapper = shallow(
    <Notifications
      appState={{ organizationsEnabled: false }}
      fetchOrganizations={jest.fn()}
      {...props}
    />
  );
  await waitAndUpdate(wrapper);
  return wrapper;
}
