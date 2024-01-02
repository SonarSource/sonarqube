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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { getNotifications } from '../../../../api/notifications';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockNotification } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { NotificationGlobalType, NotificationProjectType } from '../../../../types/notifications';
import ProjectNotifications from '../ProjectNotifications';

jest.mock('../../../../api/notifications', () => ({
  addNotification: jest.fn().mockResolvedValue(undefined),
  removeNotification: jest.fn().mockResolvedValue(undefined),
  getNotifications: jest.fn(),
}));

beforeAll(() => {
  jest.mocked(getNotifications).mockResolvedValue({
    channels: ['channel1'],
    globalTypes: [NotificationGlobalType.MyNewIssues],
    notifications: [
      mockNotification({}),
      mockNotification({ type: NotificationProjectType.NewAlerts }),
    ],
    perProjectTypes: [NotificationProjectType.NewAlerts, NotificationProjectType.NewIssues],
  });
});

it('should render correctly', async () => {
  const user = userEvent.setup();
  renderProjectNotifications();

  expect(
    await screen.findByText('project_information.project_notifications.title'),
  ).toBeInTheDocument();
  expect(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewAlerts.project',
    ),
  ).toBeChecked();

  expect(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewIssues.project',
    ),
  ).not.toBeChecked();

  // Toggle New Alerts
  await user.click(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewAlerts.project',
    ),
  );

  expect(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewAlerts.project',
    ),
  ).not.toBeChecked();

  // Toggle New Issues
  await user.click(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewIssues.project',
    ),
  );

  expect(
    screen.getByLabelText(
      'notification.dispatcher.descrption_x.notification.dispatcher.NewIssues.project',
    ),
  ).toBeChecked();
});

function renderProjectNotifications() {
  return renderComponent(
    <ProjectNotifications component={mockComponent({ key: 'foo', name: 'Foo' })} />,
  );
}
