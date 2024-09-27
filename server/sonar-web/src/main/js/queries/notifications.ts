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

import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { uniqWith } from 'lodash';
import { addNotification, getNotifications, removeNotification } from '../api/notifications';
import { Notification } from '../types/notifications';
import { createQueryHook, StaleTime } from './common';

const KEY_PREFIX = 'notifications';

const notificationQuery = queryOptions({
  queryKey: [KEY_PREFIX],
  queryFn: () => getNotifications(),
  staleTime: StaleTime.NEVER,
});

function areNotificationsEqual(a: Notification, b: Notification) {
  return a.channel === b.channel && a.type === b.type && a.project === b.project;
}

export const useNotificationsQuery = createQueryHook(() => notificationQuery);

export function useAddNotificationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: addNotification,
    onSuccess: (_, { channel, type, project }) => {
      queryClient.setQueryData(notificationQuery.queryKey, (previous) => {
        if (previous === undefined) {
          return previous;
        }
        return {
          ...previous,
          notifications: uniqWith(
            [...previous.notifications, { channel, type, project }],
            areNotificationsEqual,
          ),
        };
      });
      queryClient.invalidateQueries({ queryKey: [KEY_PREFIX] });
    },
  });
}

export function useRemoveNotificationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: removeNotification,
    onSuccess: (_, removed) => {
      queryClient.setQueryData(notificationQuery.queryKey, (previous) => {
        if (previous === undefined) {
          return previous;
        }
        return {
          ...previous,
          notifications: previous.notifications.filter(
            (notification) => !areNotificationsEqual(notification, removed),
          ),
        };
      });
      queryClient.invalidateQueries({ queryKey: [KEY_PREFIX] });
    },
  });
}
