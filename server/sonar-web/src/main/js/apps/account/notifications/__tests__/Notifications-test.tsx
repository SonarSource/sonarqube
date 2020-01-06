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
import GlobalNotifications from '../GlobalNotifications';
import { Notifications } from '../Notifications';
import Projects from '../Projects';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot();
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ notifications: [] })).toMatchSnapshot();
});

it('should add and remove global notifications', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const notification = { channel: 'channel2', type: 'type-global' };
  const wrapper = shallowRender({ addNotification, removeNotification });

  wrapper
    .find(GlobalNotifications)
    .props()
    .addNotification(notification);
  expect(addNotification).toBeCalledWith(notification);

  wrapper
    .find(GlobalNotifications)
    .props()
    .removeNotification(notification);
  expect(removeNotification).toBeCalledWith(notification);
});

it('should add and remove project notification', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const notification = {
    channel: 'channel2',
    type: 'type-common',
    project: 'qux'
  };
  const wrapper = shallowRender({ addNotification, removeNotification });

  wrapper
    .find(Projects)
    .props()
    .addNotification(notification);
  expect(addNotification).toBeCalledWith(notification);

  wrapper
    .find(Projects)
    .props()
    .removeNotification(notification);
  expect(removeNotification).toBeCalledWith(notification);
});

function shallowRender(props = {}) {
  return shallow(
    <Notifications
      addNotification={jest.fn()}
      channels={['channel1', 'channel2']}
      globalTypes={['type-global', 'type-common']}
      loading={false}
      notifications={[
        {
          channel: 'channel1',
          type: 'type-global',
          project: 'foo',
          projectName: 'Foo',
          organization: 'org'
        },
        {
          channel: 'channel1',
          type: 'type-common',
          project: 'bar',
          projectName: 'Bar',
          organization: 'org'
        },
        {
          channel: 'channel2',
          type: 'type-common',
          project: 'qux',
          projectName: 'Qux',
          organization: 'org'
        }
      ]}
      perProjectTypes={['type-common']}
      removeNotification={jest.fn()}
      {...props}
    />
  );
}
