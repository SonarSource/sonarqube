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
import { User } from '../../types/users';
import {
  CreateUserParams,
  DeactivateUserParams,
  SearchUsersParams,
  UpdateUserParams,
  createUser,
  deactivateUser,
  searchUsers,
  updateUser,
} from '../users';

export function useUsersQueries(
  searchParam: Omit<SearchUsersParams, 'p' | 'ps'>,
  numberOfPages: number
) {
  type QueryKey = ['user', 'list', number, Omit<SearchUsersParams, 'p' | 'ps'>];
  const results = useQueries({
    queries: range(1, numberOfPages + 1).map((page: number) => ({
      queryKey: ['user', 'list', page, searchParam],
      queryFn: ({ queryKey: [_u, _l, page, searchParam] }: QueryFunctionContext<QueryKey>) =>
        searchUsers({ ...searchParam, p: page }),
    })),
  });

  return results.reduce(
    (acc, { data, isLoading }) => ({
      users: acc.users.concat(data?.users ?? []),
      total: data?.paging.total,
      isLoading: acc.isLoading || isLoading,
    }),
    { users: [] as User[], total: 0, isLoading: false }
  );
}

export function useInvalidateUsersList() {
  const queryClient = useQueryClient();

  return () => queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
}

export function useCreateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateUserParams) => {
      await createUser(data);
    },
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: UpdateUserParams) => {
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
    mutationFn: async (data: DeactivateUserParams) => {
      await deactivateUser(data);
    },
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['user', 'list'] });
    },
  });
}
