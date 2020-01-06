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
import ProjectNotifications from '../ProjectNotifications';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ collapsed: true })).toMatchSnapshot();
});

it('should call `addNotification` and `removeNotification`', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const wrapper = shallowRender({ addNotification, removeNotification });
  const notificationsList = wrapper.find('NotificationsList');

  notificationsList.prop<Function>('onAdd')({ channel: 'channel2', type: 'type1' });
  expect(addNotification).toHaveBeenCalledWith({
    channel: 'channel2',
    project: 'foo',
    projectName: 'Foo',
    type: 'type1'
  });

  jest.resetAllMocks();

  notificationsList.prop<Function>('onRemove')({ channel: 'channel1', type: 'type1' });
  expect(removeNotification).toHaveBeenCalledWith({
    channel: 'channel1',
    project: 'foo',
    projectName: 'Foo',
    type: 'type1'
  });
});

function shallowRender(props = {}) {
  const project = { project: 'foo', projectName: 'Foo' };
  return shallow(
    <ProjectNotifications
      addNotification={jest.fn()}
      channels={['channel1', 'channel2']}
      collapsed={false}
      notifications={[
        { ...project, channel: 'channel1', type: 'type1' },
        { ...project, channel: 'channel1', type: 'type2' },
        { ...project, channel: 'channel2', type: 'type2' }
      ]}
      project={project}
      removeNotification={jest.fn()}
      types={['type1', 'type2']}
      {...props}
    />
  );
}
