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
import * as React from 'react';
import { shallow } from 'enzyme';
import ProjectNotifications from '../ProjectNotifications';

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
      <ProjectNotifications
        addNotification={jest.fn()}
        channels={channels}
        notifications={notifications}
        project={{ key: 'foo', name: 'Foo', organization: 'org' }}
        removeNotification={jest.fn()}
        types={types}
      />
    )
  ).toMatchSnapshot();
});

it('should call `addNotification` and `removeNotification`', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const wrapper = shallow(
    <ProjectNotifications
      addNotification={addNotification}
      channels={channels}
      notifications={notifications}
      project={{ key: 'foo', name: 'Foo', organization: 'org' }}
      removeNotification={removeNotification}
      types={types}
    />
  );
  const notificationsList = wrapper.find('NotificationsList');

  notificationsList.prop<Function>('onAdd')({ channel: 'channel2', type: 'type1' });
  expect(addNotification).toHaveBeenCalledWith({
    channel: 'channel2',
    organization: 'org',
    project: 'foo',
    projectName: 'Foo',
    type: 'type1'
  });

  jest.resetAllMocks();

  notificationsList.prop<Function>('onRemove')({ channel: 'channel1', type: 'type1' });
  expect(removeNotification).toHaveBeenCalledWith({
    channel: 'channel1',
    type: 'type1',
    project: 'foo'
  });
});
