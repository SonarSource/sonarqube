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
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { range } from 'lodash';
import {
  createGroup,
  deleteGroup,
  getUsersGroups,
  getUsersInGroup,
  updateGroup,
} from '../api/user_groups';
import { Group } from '../types/types';

const STALE_TIME = 4 * 60 * 1000;

export function useGroupsQueries(
  getParams: Omit<Parameters<typeof getUsersGroups>[0], 'pageSize' | 'pageIndex'>,
  numberOfPages: number,
) {
  type QueryKey = [
    'group',
    'list',
    number,
    Omit<Parameters<typeof getUsersGroups>[0], 'pageSize' | 'pageIndex'>,
  ];
  const results = useQueries({
    queries: range(1, numberOfPages + 1).map((page: number) => ({
      queryKey: ['group', 'list', page, getParams],
      queryFn: ({ queryKey: [_u, _l, page, getParams] }: QueryFunctionContext<QueryKey>) =>
        getUsersGroups({ ...getParams, p: page }),
      staleTime: STALE_TIME,
    })),
  });

  return results.reduce<{ groups: Group[]; total: number | undefined; isLoading: boolean }>(
    (acc, { data, isLoading }) => ({
      groups: acc.groups.concat(data?.groups ?? []),
      total: data?.paging.total,
      isLoading: acc.isLoading || isLoading,
    }),
    { groups: [], total: 0, isLoading: false },
  );
}

export function useMembersCountQuery(name: string) {
  return useQuery({
    queryKey: ['group', name, 'members', 'total'],
    queryFn: () => getUsersInGroup({ name, ps: 1 }).then((r) => r.paging.total),
    staleTime: STALE_TIME,
  });
}

export function useCreateGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof createGroup>[0]) => createGroup(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['group', 'list'] });
    },
  });
}

export function useUpdateGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof updateGroup>[0]) => updateGroup(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['group', 'list'] });
    },
  });
}

export function useDeleteGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof deleteGroup>[0]) => deleteGroup(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['group', 'list'] });
    },
  });
}
