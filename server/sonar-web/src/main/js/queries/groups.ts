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

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createGroup,
  deleteGroup,
  getUsersGroups,
  getUsersInGroup,
  updateGroup,
} from '../api/user_groups';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';

const STALE_TIME = 4 * 60 * 1000;

export function useGroupsQueries(
  getParams: Omit<Parameters<typeof getUsersGroups>[0], 'pageSize' | 'pageIndex'>,
) {
  return useInfiniteQuery({
    queryKey: ['group', 'list', getParams],
    queryFn: ({ pageParam = 1 }) => getUsersGroups({ ...getParams, pageIndex: pageParam }),
    getNextPageParam,
    getPreviousPageParam,
  });
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
    mutationFn: ({
      id,
      data,
    }: {
      id: Parameters<typeof updateGroup>[0];
      data: Parameters<typeof updateGroup>[1];
    }) => updateGroup(id, data),
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
