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

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { generateToken, getTokens, revokeToken } from '../api/user-tokens';
import { deleteUser, dismissNotice, getUsers, postUser, updateUser } from '../api/users';
import { useCurrentUser } from '../app/components/current-user/CurrentUserContext';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { UserToken } from '../types/token';
import { NoticeType, RestUserBase } from '../types/users';

const STALE_TIME = 4 * 60 * 1000;

export function useUsersQueries<U extends RestUserBase>(
  getParams: Omit<Parameters<typeof getUsers>[0], 'pageSize' | 'pageIndex'>,
  enabled = true,
) {
  return useInfiniteQuery({
    queryKey: ['user', 'list', getParams],
    queryFn: ({ pageParam }) => getUsers<U>({ ...getParams, pageIndex: pageParam }),
    getNextPageParam,
    getPreviousPageParam,
    enabled,
    initialPageParam: 1,
  });
}

export function useUserTokensQuery(login: string) {
  return useQuery({
    queryKey: ['user', login, 'tokens'],
    queryFn: () => getTokens(login),
    staleTime: STALE_TIME,
  });
}

export function usePostUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof postUser>[0]) => postUser(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      data: Parameters<typeof updateUser>[1];
      id: Parameters<typeof updateUser>[0];
    }) => updateUser(id, data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useDeactivateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof deleteUser>[0]) => deleteUser(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useGenerateTokenMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof generateToken>[0] & { projectName?: string }) =>
      generateToken(data),
    onSuccess(data, variables) {
      queryClient.setQueryData<UserToken[]>(['user', data.login, 'tokens'], (oldData) => {
        const newData = {
          ...data,
          project:
            variables.projectKey && variables.projectName
              ? { key: variables.projectKey, name: variables.projectName }
              : undefined,
        };
        return oldData ? [...oldData, newData] : [newData];
      });
    },
  });
}

export function useRevokeTokenMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof revokeToken>[0]) => revokeToken(data),
    onSuccess(_, data) {
      queryClient.setQueryData<UserToken[]>(['user', data.login, 'tokens'], (oldData) =>
        oldData ? oldData.filter((token) => token.name !== data.name) : undefined,
      );
    },
  });
}

export function useDismissNoticeMutation() {
  const { updateDismissedNotices } = useCurrentUser();

  return useMutation({
    mutationFn: (data: NoticeType) => dismissNotice(data),
    onSuccess(_, data) {
      updateDismissedNotices(data, true);
    },
  });
}
