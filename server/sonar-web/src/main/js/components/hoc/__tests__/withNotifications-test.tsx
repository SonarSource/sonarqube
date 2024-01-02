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
import { addNotification, getNotifications, removeNotification } from '../../../api/notifications';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { withNotifications, WithNotificationsProps } from '../withNotifications';

jest.mock('../../../api/notifications', () => ({
  addNotification: jest.fn().mockResolvedValue({}),
  getNotifications: jest.fn(() =>
    Promise.resolve({
      channels: ['channel1', 'channel2'],
      globalTypes: ['type-global', 'type-common'],
      notifications: [
        {
          channel: 'channel1',
          type: 'type-global',
          project: 'foo',
          projectName: 'Foo',
        },
        {
          channel: 'channel1',
          type: 'type-common',
          project: 'bar',
          projectName: 'Bar',
        },
        {
          channel: 'channel2',
          type: 'type-common',
          project: 'qux',
          projectName: 'Qux',
        },
      ],
      perProjectTypes: ['type-common'],
    })
  ),
  removeNotification: jest.fn().mockResolvedValue({}),
}));

class X extends React.Component<WithNotificationsProps> {
  render() {
    return <div />;
  }
}

beforeEach(() => {
  jest.clearAllMocks();
});

it('should fetch notifications and render', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getNotifications).toHaveBeenCalled();
});

it('should add and remove a notification', () => {
  const wrapper = shallowRender();
  const notification = {
    channel: 'EmailNotificationChannel',
    project: 'foo',
    type: 'SQ-MyNewIssues',
  };

  wrapper.prop('addNotification')(notification);
  expect(addNotification).toHaveBeenCalledWith(notification);

  wrapper.prop('removeNotification')(notification);
  expect(removeNotification).toHaveBeenCalledWith(notification);
});

function shallowRender() {
  const UnderTest = withNotifications<{}>(X);
  return shallow(<UnderTest />);
}
