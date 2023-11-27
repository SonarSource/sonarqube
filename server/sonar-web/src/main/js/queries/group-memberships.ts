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
  addGroupMembership,
  getGroupMemberships,
  removeGroupMembership,
} from '../api/group-memberships';
import { getUsers } from '../api/users';
import { SelectListFilter } from '../components/controls/SelectList';
import { translateWithParameters } from '../helpers/l10n';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { RestUserDetailed } from '../types/users';

const STALE_TIME = 4 * 60 * 1000;
const DOMAIN = 'group-memberships';
const GROUP_SUB_DOMAIN = 'users-of-group';

export function useGroupMembersQuery(params: {
  filter?: SelectListFilter;
  q?: string;
  groupId: string;
  pageIndex?: number;
}) {
  return useInfiniteQuery({
    queryKey: [DOMAIN, GROUP_SUB_DOMAIN, params],
    queryFn: async ({ pageParam = 1 }) => {
      if (params.filter === SelectListFilter.All) {
        const result = await getUsers<RestUserDetailed>({
          q: params.q ?? '',
          pageIndex: pageParam,
        });
        const isSelected = async (userId: string, groupId: string) => {
          const memberships = await getGroupMemberships({ userId, groupId, pageSize: 0 });
          return memberships.page.total > 0;
        };
        return {
          users: await Promise.all(
            result.users.map(async (u) => ({
              ...u,
              selected: await isSelected(u.id, params.groupId),
            })),
          ),
          page: result.page,
        };
      }
      const isSelected = params.filter === SelectListFilter.Selected || params.filter === undefined;
      return getUsers<RestUserDetailed>({
        q: params.q ?? '',
        [isSelected ? 'groupId' : 'groupId!']: params.groupId,
        pageIndex: pageParam,
      }).then((res) => ({
        users: res.users.map((u) => ({
          ...u,
          selected: isSelected,
        })),
        page: res.page,
      }));
    },
    getNextPageParam,
    getPreviousPageParam,
  });
}

export function useGroupMembersCountQuery(groupId: string) {
  return useQuery({
    queryKey: [DOMAIN, GROUP_SUB_DOMAIN, 'count', groupId],
    queryFn: () => getGroupMemberships({ groupId, pageSize: 0 }).then((r) => r.page.total),
    staleTime: STALE_TIME,
  });
}

export function useAddGroupMembershipMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof addGroupMembership>[0]) => addGroupMembership(data),
    onSuccess(_, data) {
      queryClient.setQueryData<number>(
        [DOMAIN, GROUP_SUB_DOMAIN, 'count', data.groupId],
        (oldData) => (oldData !== undefined ? oldData + 1 : undefined),
      );
    },
  });
}

export function useRemoveGroupMembershipMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ userId, groupId }: { userId: string; groupId: string }) => {
      const memberships = await getGroupMemberships({ userId, groupId, pageSize: 1 });
      if (!memberships.page.total) {
        throw new Error(
          translateWithParameters('group_membership.remove_user.error', userId, groupId),
        );
      }
      return removeGroupMembership(memberships.groupMemberships[0].id);
    },
    onSuccess(_, data) {
      queryClient.setQueryData<number>(
        [DOMAIN, GROUP_SUB_DOMAIN, 'count', data.groupId],
        (oldData) => (oldData !== undefined ? oldData - 1 : undefined),
      );
    },
  });
}
