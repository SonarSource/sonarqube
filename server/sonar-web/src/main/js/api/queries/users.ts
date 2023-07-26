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
import {
  CreateUserParams,
  DeactivateUserParams,
  GetUsersParams,
  Permission,
  RestUser,
  UpdateUserParams,
  createUser,
  deactivateUser,
  getUsers,
  updateUser,
} from '../users';

export function useUsersQueries<P extends Permission>(
  getParams: Omit<GetUsersParams, 'pageSize' | 'pageIndex'>,
  numberOfPages: number
) {
  type QueryKey = ['user', 'list', number, Omit<GetUsersParams, 'pageSize' | 'pageIndex'>];
  const results = useQueries({
    queries: range(1, numberOfPages + 1).map((page: number) => ({
      queryKey: ['user', 'list', page, getParams],
      queryFn: ({ queryKey: [_u, _l, page, getParams] }: QueryFunctionContext<QueryKey>) =>
        getUsers<P>({ ...getParams, pageIndex: page }),
    })),
  });

  return results.reduce(
    (acc, { data, isLoading }) => ({
      users: acc.users.concat(data?.users ?? []),
      total: data?.pageRestResponse.total,
      isLoading: acc.isLoading || isLoading,
    }),
    { users: [] as RestUser<P>[], total: 0, isLoading: false }
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
