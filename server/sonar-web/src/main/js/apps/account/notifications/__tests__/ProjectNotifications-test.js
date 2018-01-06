/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import { UnconnectedProjectNotifications } from '../ProjectNotifications';
import NotificationsList from '../NotificationsList';

const channels = ['channel1', 'channel2'];
const types = ['type1', 'type2'];
const notifications = [
  { channel: 'channel1', type: 'type1' },
  { channel: 'channel1', type: 'type2' },
  { channel: 'channel2', type: 'type2' }
];

it('should match snapshot', () => {
  expect(
    shallow(
      <UnconnectedProjectNotifications
        project={{ key: 'foo', name: 'Foo' }}
        notifications={notifications}
        channels={channels}
        types={types}
        addNotification={jest.fn()}
        removeNotification={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should call `addNotification` and `removeNotification`', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const wrapper = shallow(
    <UnconnectedProjectNotifications
      project={{ key: 'foo', name: 'Foo' }}
      notifications={notifications}
      channels={channels}
      types={types}
      addNotification={addNotification}
      removeNotification={removeNotification}
    />
  );
  const notificationsList = wrapper.find(NotificationsList);

  notificationsList.prop('onAdd')({ channel: 'channel2', type: 'type1' });
  expect(addNotification).toHaveBeenCalledWith({
    channel: 'channel2',
    type: 'type1',
    project: 'foo',
    projectName: 'Foo'
  });

  jest.resetAllMocks();

  notificationsList.prop('onRemove')({ channel: 'channel1', type: 'type1' });
  expect(removeNotification).toHaveBeenCalledWith({
    channel: 'channel1',
    type: 'type1',
    project: 'foo'
  });
});
