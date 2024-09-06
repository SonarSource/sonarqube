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
import { queryOptions, useQuery, useQueryClient } from '@tanstack/react-query';
import { groupBy, omit } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { getTasksForComponent } from '../api/ce';
import {
  getBreadcrumbs,
  getComponent,
  getComponentData,
  getComponentForSourceViewer,
} from '../api/components';
import { getBranchLikeQuery } from '../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../types/branch-like';
import { Component, Measure } from '../types/types';
import { StaleTime, createQueryHook } from './common';

const TASK_RETRY = 10_000;

type QueryKeyData = {
  branchParameters: BranchParameters;
  metricKeys: string[];
};

function extractQueryKeyData(queryKey: string[]): { data?: QueryKeyData; key: string } {
  const [, key, , data] = queryKey;
  return { key, data: JSON.parse(data ?? 'null') };
}

export function useTaskForComponentQuery(component: Component) {
  return useQuery({
    queryKey: ['component', component.key, 'tasks'],
    queryFn: ({ queryKey }) => {
      const { key } = extractQueryKeyData(queryKey);
      return getTasksForComponent(key);
    },
    refetchInterval: TASK_RETRY,
  });
}

export const useComponentQuery = createQueryHook(
  ({ component, metricKeys, ...params }: Parameters<typeof getComponent>[0]) => {
    const queryClient = useQueryClient();

    return queryOptions({
      queryKey: ['component', component, 'measures', { metricKeys, params }],
      queryFn: async () => {
        const result = await getComponent({
          component,
          metricKeys,
          ...params,
        });
        const measuresMapByMetricKey = groupBy(result.component.measures, 'metric');
        metricKeys.split(',').forEach((metricKey) => {
          const measure = measuresMapByMetricKey[metricKey]?.[0] ?? null;
          queryClient.setQueryData<Measure>(
            ['measures', 'details', result.component.key, metricKey],
            measure,
          );
        });
        return result;
      },
      staleTime: StaleTime.LONG,
    });
  },
);

export const useComponentBreadcrumbsQuery = createQueryHook(
  ({ component, ...params }: Parameters<typeof getBreadcrumbs>[0]) => {
    return queryOptions({
      queryKey: ['component', component, 'breadcrumbs', params],
      queryFn: () => getBreadcrumbs({ component, ...params }),
      staleTime: StaleTime.LONG,
    });
  },
);

export const useComponentDataQuery = createQueryHook(
  (data: Parameters<typeof getComponentData>[0]) => {
    return queryOptions({
      queryKey: ['component', data.component, 'component_data', omit(data, 'component')],
      queryFn: () => getComponentData(data),
      staleTime: StaleTime.LONG,
    });
  },
);

export function useComponentForSourceViewer(
  fileKey: string,
  branchLike?: BranchLike,
  enabled = true,
) {
  return useQuery({
    queryKey: ['component', 'source-viewer', fileKey, branchLike] as const,
    queryFn: ({ queryKey: [_1, _2, fileKey, branchLike] }) =>
      getComponentForSourceViewer({ component: fileKey, ...getBranchLikeQuery(branchLike) }),
    staleTime: Infinity,
    enabled,
  });
}
