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

import {
  infiniteQueryOptions,
  queryOptions,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { getScannableProjects, searchProjects } from '../api/components';
import { createProject, deleteProject } from '../api/project-management';
import { Query } from '../apps/projects/query';
import { convertToQueryData, defineFacets } from '../apps/projects/utils';
import { getNextPagingParam } from '../helpers/react-query';
import { createInfiniteQueryHook, createQueryHook, StaleTime } from './common';
import { invalidateMeasuresByComponentKey } from './measures';
import { useCurrentUser } from '../app/components/current-user/CurrentUserContext';
import { addGlobalErrorMessage } from '~design-system';
import { Navigate, NavigateFunction, To } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { replace } from 'lodash';
import { useEffect } from 'react';

export const PROJECTS_PAGE_SIZE = 50;

export const useProjectsQuery = createInfiniteQueryHook(
  ({
    isFavorite,
    query,
    pageIndex = 1,
    isStandardMode,
  }: {
    isFavorite: boolean;
    isStandardMode: boolean;
    pageIndex?: number;
    query: Query;
  }) => {
    const {currentUser} = useCurrentUser();
    if(query?.organization && currentUser.platformOrgs?.includes(query.organization))
    {
      const navigate = useNavigate();
  useEffect(() => {
    navigate('/account', { replace: true });
  }, [navigate]);
    }
    const queryClient = useQueryClient();
    const data = convertToQueryData(query, isFavorite, isStandardMode, {
      organization: query.organization,
      ps: PROJECTS_PAGE_SIZE,
      facets: defineFacets(query, isStandardMode).join(),
      f: 'analysisDate,leakPeriodDate',
    });
    
    return infiniteQueryOptions({
      queryKey: ['project', 'list', data] as const,
      queryFn: async ({ pageParam: pageIndex }) => {
        try {
          const response = await searchProjects({ ...data, p: pageIndex });
          response.components.forEach((project) => {
            queryClient.setQueryData(['project', 'details', project.key], project);
          });
          return response;
        } catch (error: any) {
          if (error?.status == 403) {
            window.location.href = '/account';
          }
          throw error;
        }
      },
      staleTime: StaleTime.LONG,
      getNextPageParam: getNextPagingParam,
      getPreviousPageParam: getNextPagingParam,
      initialPageParam: pageIndex,
    });
  },
);

export const useProjectQuery = createQueryHook((key: string) => {
  return queryOptions({
    queryKey: ['project', 'details', key],
    queryFn: ({ queryKey: [_1, _2, key] }) => searchProjects({ filter: `query=${key}` }),
  });
});

export const useMyScannableProjectsQuery = createQueryHook(() => {
  return queryOptions({
    queryKey: ['project', 'my-scannable'],
    queryFn: () => getScannableProjects(),
    staleTime: StaleTime.NEVER,
  });
});

export function useDeleteProjectMutation(organization: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => deleteProject(organization, key),
    onSuccess: (_, key) => {
      invalidateMeasuresByComponentKey(key, queryClient);
    },
  });
}

export function useCreateProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof createProject>[0]) => createProject(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['project', 'list'] });
    },
  });
}
