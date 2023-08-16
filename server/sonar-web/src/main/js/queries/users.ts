/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import {
  QueryFunctionContext,
  useMutation,
  useQueries,
  useQueryClient,
} from '@tanstack/react-query';
import { range } from 'lodash';
import { deleteUser, getUsers, postUser, updateUser } from '../api/users';
import { RestUserBase } from '../types/users';

const STALE_TIME = 4 * 60 * 1000;

export function useUsersQueries<U extends RestUserBase>(
  getParams: Omit<Parameters<typeof getUsers>[0], 'pageSize' | 'pageIndex'>,
  numberOfPages: number
) {
  type QueryKey = [
    'user',
    'list',
    number,
    Omit<Parameters<typeof getUsers>[0], 'pageSize' | 'pageIndex'>
  ];
  const results = useQueries({
    queries: range(1, numberOfPages + 1).map((page: number) => ({
      queryKey: ['user', 'list', page, getParams],
      queryFn: ({ queryKey: [_u, _l, page, getParams] }: QueryFunctionContext<QueryKey>) =>
        getUsers<U>({ ...getParams, pageIndex: page }),
      staleTime: STALE_TIME,
    })),
  });

  return results.reduce(
    (acc, { data, isLoading }) => ({
      users: acc.users.concat(data?.users ?? []),
      total: data?.page.total,
      isLoading: acc.isLoading || isLoading,
    }),
    { users: [] as U[], total: 0, isLoading: false }
  );
}

export function useInvalidateUsersList() {
  const queryClient = useQueryClient();

  return () => queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
}

export function usePostUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: Parameters<typeof postUser>[0]) => {
      await postUser(data);
    },
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: Parameters<typeof updateUser>[0]) => {
      await updateUser(data);
    },
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useDeactivateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: Parameters<typeof deleteUser>[0]) => {
      await deleteUser(data);
    },
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}
