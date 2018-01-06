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
// @flow
import * as api from '../../../api/notifications';
/*:: import type { GetNotificationsResponse } from '../../../api/notifications'; */
import { onFail, fetchOrganizations } from '../../../store/rootActions';
import {
  receiveNotifications,
  addNotification as addNotificationAction,
  removeNotification as removeNotificationAction
} from '../../../store/notifications/duck';
/*:: import type { Notification } from '../../../store/notifications/duck'; */

export const fetchNotifications = () => (dispatch /*: Function */) => {
  const onFulfil = (response /*: GetNotificationsResponse */) => {
    const organizations = response.notifications
      .filter(n => n.organization)
      .map(n => n.organization);

    dispatch(fetchOrganizations(organizations)).then(() => {
      dispatch(
        receiveNotifications(
          response.notifications,
          response.channels,
          response.globalTypes,
          response.perProjectTypes
        )
      );
    });
  };

  return api.getNotifications().then(onFulfil, onFail(dispatch));
};

export const addNotification = (n /*: Notification */) => (dispatch /*: Function */) =>
  api
    .addNotification(n.channel, n.type, n.project)
    .then(() => dispatch(addNotificationAction(n)), onFail(dispatch));

export const removeNotification = (n /*: Notification */) => (dispatch /*: Function */) =>
  api
    .removeNotification(n.channel, n.type, n.project)
    .then(() => dispatch(removeNotificationAction(n)), onFail(dispatch));
