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
import { mockComponent } from '../../../../../../../helpers/mocks/component';
import {
  NotificationGlobalType,
  NotificationProjectType,
} from '../../../../../../../types/notifications';
import { ProjectNotifications } from '../ProjectNotifications';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useEffect: jest.fn().mockImplementation((f) => f()),
    useRef: jest.fn().mockReturnValue({ current: document.createElement('h3') }),
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should add and remove a notification for the project', () => {
  const addNotification = jest.fn();
  const removeNotification = jest.fn();
  const wrapper = shallowRender({ addNotification, removeNotification });
  const notification = {
    channel: 'EmailNotificationChannel',
    type: 'SQ-MyNewIssues',
  };

  wrapper.find('NotificationsList').prop<Function>('onAdd')(notification);
  expect(addNotification).toHaveBeenCalledWith({ ...notification, project: 'foo' });

  wrapper.find('NotificationsList').prop<Function>('onRemove')(notification);
  expect(removeNotification).toHaveBeenCalledWith({ ...notification, project: 'foo' });
});

it('should set focus on the heading when rendered', () => {
  const fakeElement = document.createElement('h3');
  const focus = jest.fn();
  (React.useRef as jest.Mock).mockReturnValueOnce({ current: { ...fakeElement, focus } });

  shallowRender();
  expect(focus).toHaveBeenCalled();
});

function shallowRender(props = {}) {
  return shallow(
    <ProjectNotifications
      addNotification={jest.fn()}
      channels={['channel1', 'channel2']}
      component={mockComponent({ key: 'foo' })}
      globalTypes={[NotificationGlobalType.CeReportTaskFailure, NotificationGlobalType.NewAlerts]}
      loading={false}
      notifications={[
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
      ]}
      perProjectTypes={[NotificationProjectType.CeReportTaskFailure]}
      removeNotification={jest.fn()}
      {...props}
    />
  );
}
