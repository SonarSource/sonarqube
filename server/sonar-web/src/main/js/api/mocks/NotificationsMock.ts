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

import { cloneDeep } from 'lodash';
import {
  AddRemoveNotificationParameters,
  Notification,
  NotificationGlobalType,
  NotificationProjectType,
  NotificationsResponse,
} from '../../types/notifications';
import { addNotification, getNotifications, removeNotification } from '../notifications';

jest.mock('../notifications');

/* Constants */
const channels = ['EmailNotificationChannel'];
const defaultNotifications: Notification[] = [
  { channel: 'EmailNotificationChannel', type: 'ChangesOnMyIssue' },
];

export default class NotificationsMock {
  notifications: Notification[];

  constructor() {
    this.notifications = cloneDeep(defaultNotifications);

    (getNotifications as jest.Mock).mockImplementation(this.handleGetNotifications);
    (addNotification as jest.Mock).mockImplementation(this.handleAddNotification);
    (removeNotification as jest.Mock).mockImplementation(this.handleRemoveNotification);
  }

  handleGetNotifications: () => Promise<NotificationsResponse> = () => {
    return Promise.resolve({
      channels: [...channels],
      globalTypes: Object.values(NotificationGlobalType),
      notifications: cloneDeep(this.notifications),
      perProjectTypes: Object.values(NotificationProjectType),
    });
  };

  handleAddNotification = (params: AddRemoveNotificationParameters) => {
    this.notifications.push(params);

    return Promise.resolve();
  };

  handleRemoveNotification = (params: AddRemoveNotificationParameters) => {
    const index = this.notifications.findIndex(
      (n) => n.project === params.project && n.type === params.type && n.channel === params.channel,
    );

    if (index < 0) {
      return Promise.reject({ errors: [{ msg: "Notification doesn't exist" }] });
    }

    this.notifications.splice(index, 1);

    return Promise.resolve();
  };

  reset = () => {
    this.notifications = cloneDeep(defaultNotifications);
  };
}
