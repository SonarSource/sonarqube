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
  Query,
  QueryClient,
  queryOptions,
  useQueries,
  useQueryClient,
} from '@tanstack/react-query';
import { chunk, groupBy, isUndefined, omitBy } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { getComponentTree } from '../api/components';
import {
  getMeasures,
  getMeasuresForProjects,
  getMeasuresWithPeriodAndMetrics,
} from '../api/measures';
import { getAllTimeMachineData } from '../api/time-machine';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { getBranchLikeQuery } from '../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../types/branch-like';
import { Measure } from '../types/types';
import { createInfiniteQueryHook, createQueryHook, StaleTime } from './common';
import { PROJECTS_PAGE_SIZE } from './projects';

const measureQueryKeys = {
  all: () => ['measures'] as const,
  history: (componentKey: string) => [...measureQueryKeys.all(), 'history', componentKey] as const,
  component: (componentKey: string) =>
    [...measureQueryKeys.all(), 'component', componentKey] as const,
  details: (componentKey: string) => [...measureQueryKeys.all(), 'details', componentKey] as const,
  list: (componentKey: string) => [...measureQueryKeys.all(), 'list', componentKey] as const,
};

const projectsListPredicate = (query: Query, componentKey: string) =>
  query.queryKey[0] === 'measures' &&
  query.queryKey[1] === 'list' &&
  query.queryKey[2] === 'projects' &&
  Array.isArray(query.queryKey[3]) &&
  query.queryKey[3].includes(componentKey);

export const invalidateMeasuresByComponentKey = (
  componentKey: string,
  queryClient: QueryClient,
) => {
  queryClient.invalidateQueries({ queryKey: measureQueryKeys.history(componentKey) });
  queryClient.invalidateQueries({ queryKey: measureQueryKeys.component(componentKey) });
  queryClient.invalidateQueries({ queryKey: measureQueryKeys.details(componentKey) });
  queryClient.invalidateQueries({ queryKey: measureQueryKeys.list(componentKey) });
  queryClient.invalidateQueries({
    predicate: (query) => projectsListPredicate(query, componentKey),
  });
};

export const removeMeasuresByComponentKey = (componentKey: string, queryClient: QueryClient) => {
  queryClient.removeQueries({ queryKey: measureQueryKeys.history(componentKey) });
  queryClient.removeQueries({ queryKey: measureQueryKeys.component(componentKey) });
  queryClient.removeQueries({ queryKey: measureQueryKeys.details(componentKey) });
  queryClient.removeQueries({ queryKey: measureQueryKeys.list(componentKey) });
  queryClient.removeQueries({
    predicate: (query) => projectsListPredicate(query, componentKey),
  });
};

export const invalidateAllMeasures = (queryClient: QueryClient) => {
  queryClient.invalidateQueries({ queryKey: ['measures'] });
};

export const useAllMeasuresHistoryQuery = createQueryHook(
  ({
    component,
    branchParams,
    metrics,
  }: Omit<Parameters<typeof getAllTimeMachineData>[0], 'to' | 'from' | 'p'> & {
    branchParams?: BranchParameters;
  }) => {
    return queryOptions({
      queryKey: ['measures', 'history', component, branchParams, metrics],
      queryFn: () => {
        if (metrics.length <= 0) {
          return Promise.resolve({
            measures: [],
            paging: { pageIndex: 1, pageSize: 1, total: 0 },
          });
        }
        return getAllTimeMachineData({ component, metrics, ...branchParams, p: 1 });
      },
    });
  },
);

export const useMeasuresComponentQuery = createQueryHook(
  ({
    componentKey,
    metricKeys,
    branchLike,
  }: {
    branchLike?: BranchLike;
    componentKey: string;
    metricKeys: string[];
  }) => {
    const queryClient = useQueryClient();
    const branchLikeQuery = getBranchLikeQuery(branchLike);

    return queryOptions({
      queryKey: [
        'measures',
        'component',
        componentKey,
        'branchLike',
        { ...branchLikeQuery },
        metricKeys,
      ],
      queryFn: async () => {
        const data = await getMeasuresWithPeriodAndMetrics(
          componentKey,
          metricKeys,
          branchLikeQuery,
        );
        metricKeys.forEach((metricKey) => {
          const measure =
            data.component.measures?.find((measure) => measure.metric === metricKey) ?? null;
          queryClient.setQueryData<Measure | null>(
            ['measures', 'details', componentKey, 'branchLike', { ...branchLikeQuery }, metricKey],
            measure,
          );
        });

        return data;
      },
    });
  },
);

export const useComponentTreeQuery = createInfiniteQueryHook(
  ({
    strategy,
    component,
    metrics,
    additionalData,
  }: {
    additionalData: Parameters<typeof getComponentTree>[3];
    component: Parameters<typeof getComponentTree>[1];
    metrics: Parameters<typeof getComponentTree>[2];
    strategy: 'children' | 'leaves';
  }) => {
    const branchLikeQuery = omitBy(
      {
        branch: additionalData?.branch,
        pullRequest: additionalData?.pullRequest,
      },
      isUndefined,
    );

    const queryClient = useQueryClient();
    return infiniteQueryOptions({
      queryKey: ['measures', 'component', component, 'tree', strategy, { metrics, additionalData }],
      queryFn: async ({ pageParam }) => {
        const result = await getComponentTree(strategy, component, metrics, {
          ...additionalData,
          p: pageParam,
          ...branchLikeQuery,
        });

        if (result.baseComponent.measures && result.baseComponent.measures.length > 0) {
          const measuresMapByMetricKeyForBaseComponent = groupBy(
            result.baseComponent.measures,
            'metric',
          );
          metrics?.forEach((metricKey) => {
            const measure = measuresMapByMetricKeyForBaseComponent[metricKey]?.[0] ?? null;
            queryClient.setQueryData<Measure>(
              [
                'measures',
                'details',
                result.baseComponent.key,
                'branchLike',
                { ...branchLikeQuery },
                metricKey,
              ],
              measure,
            );
          });
        }

        result.components.forEach((childComponent) => {
          const measuresMapByMetricKeyForChildComponent = groupBy(
            childComponent.measures,
            'metric',
          );

          metrics?.forEach((metricKey) => {
            const measure = measuresMapByMetricKeyForChildComponent[metricKey]?.[0] ?? null;
            queryClient.setQueryData<Measure>(
              [
                'measures',
                'details',
                childComponent.key,
                'branchLike',
                { ...branchLikeQuery },
                metricKey,
              ],
              measure,
            );
          });
        });
        return result;
      },
      getNextPageParam: (data) => getNextPageParam({ page: data.paging }),
      getPreviousPageParam: (data) => getPreviousPageParam({ page: data.paging }),
      initialPageParam: 1,
      staleTime: 60_000,
    });
  },
);

export function useMeasuresForProjectsQuery({
  projectKeys,
  metricKeys,
}: {
  metricKeys: string[];
  projectKeys: string[];
}) {
  const queryClient = useQueryClient();
  return useQueries({
    queries: chunk(projectKeys, PROJECTS_PAGE_SIZE).map((projectsChunk) =>
      queryOptions({
        queryKey: ['measures', 'list', 'projects', projectsChunk, metricKeys],
        staleTime: StaleTime.SHORT,
        queryFn: async () => {
          const measures = await getMeasuresForProjects(projectsChunk, metricKeys);
          const measuresMapByProjectKey = groupBy(measures, 'component');
          projectsChunk.forEach((projectKey) => {
            const measuresForProject = measuresMapByProjectKey[projectKey] ?? [];
            const measuresMapByMetricKey = groupBy(measuresForProject, 'metric');
            metricKeys.forEach((metricKey) => {
              const measure = measuresMapByMetricKey[metricKey]?.[0] ?? null;
              queryClient.setQueryData<Measure>(
                ['measures', 'details', projectKey, 'branchLike', {}, metricKey],
                measure,
              );
            });
          });
          return measures;
        },
      }),
    ),
  });
}

export const useMeasuresAndLeakQuery = createQueryHook(
  ({
    componentKey,
    metricKeys,
    branchLike,
  }: {
    branchLike?: BranchLike;
    componentKey: string;
    metricKeys: string[];
  }) => {
    const queryClient = useQueryClient();
    const branchParameters = getBranchLikeQuery(branchLike);
    return queryOptions({
      queryKey: [
        'measures',
        'details',
        componentKey,
        'branchLike',
        { ...branchParameters },
        metricKeys,
      ],
      queryFn: async () => {
        const { component, metrics, period } = await getMeasuresWithPeriodAndMetrics(
          componentKey,
          metricKeys,
          branchParameters,
        );
        const measuresMapByMetricKey = groupBy(component.measures, 'metric');
        metricKeys.forEach((metricKey) => {
          const measure = measuresMapByMetricKey[metricKey]?.[0] ?? null;
          queryClient.setQueryData<Measure>(
            ['measures', 'details', componentKey, 'branchLike', { ...branchParameters }, metricKey],
            measure,
          );
        });
        return { component, metrics, period };
      },
    });
  },
);

export const useMeasureQuery = createQueryHook(
  ({
    componentKey,
    metricKey,
    branchLike,
  }: {
    branchLike?: BranchLike;
    componentKey: string;
    metricKey: string;
  }) => {
    const branchLikeQuery = getBranchLikeQuery(branchLike);

    return queryOptions({
      queryKey: [
        'measures',
        'details',
        componentKey,
        'branchLike',
        { ...branchLikeQuery },
        metricKey,
      ],
      queryFn: () =>
        getMeasures({ component: componentKey, metricKeys: metricKey }).then(
          (measures) => measures[0] ?? null,
        ),
      staleTime: Infinity,
    });
  },
);

export const useMeasuresQuery = createQueryHook(
  ({
    componentKey,
    metricKeys,
    branchLike,
  }: {
    branchLike?: BranchLike;
    componentKey: string;
    metricKeys: string;
  }) => {
    const queryClient = useQueryClient();
    const branchLikeQuery = getBranchLikeQuery(branchLike);

    return queryOptions({
      queryKey: [
        'measures',
        'list',
        componentKey,
        'branchLike',
        { ...branchLikeQuery },
        metricKeys,
      ],
      queryFn: async () => {
        const measures = await getMeasures({
          component: componentKey,
          metricKeys,
        });

        const measuresMapByMetricKey = groupBy(measures, 'metric');
        metricKeys.split(',').forEach((metricKey) => {
          const measure = measuresMapByMetricKey[metricKey]?.[0] ?? null;
          queryClient.setQueryData<Measure>(
            ['measures', 'details', componentKey, 'branchLike', { ...branchLikeQuery }, metricKey],
            measure,
          );
        });
        return measures;
      },
    });
  },
);
