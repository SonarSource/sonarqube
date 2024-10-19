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
import { checkMessageDismissed, MessageDismissParams, setMessageDismissed } from '../api/messages';
import { useCurrentUser } from '../app/components/current-user/CurrentUserContext';
import { isLoggedIn } from '../types/users';
import { createQueryHook } from './common';

export const useMessageDismissedQuery = createQueryHook(
  ({ messageType, projectKey }: MessageDismissParams) => {
    const { currentUser } = useCurrentUser();
    return queryOptions({
      queryKey: ['message-dismissed', projectKey, messageType],
      queryFn: () => checkMessageDismissed({ projectKey, messageType }),
      enabled: isLoggedIn(currentUser),
    });
  },
);

export function useMessageDismissedMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: setMessageDismissed,
    onSuccess: (_, { messageType, projectKey }) => {
      queryClient.setQueryData(['message-dismissed', projectKey, messageType], { dismiss: true });
      queryClient.invalidateQueries({ queryKey: ['message-dismissed', projectKey, messageType] });
    },
  });
}
