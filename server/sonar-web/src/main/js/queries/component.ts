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
  UseQueryResult,
  infiniteQueryOptions,
  queryOptions,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { groupBy, omit } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { getTasksForComponent } from '../api/ce';
import {
  getBreadcrumbs,
  getComponent,
  getComponentData,
  getComponentTree,
} from '../api/components';
import { getMeasuresWithMetrics } from '../api/measures';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { MetricKey } from '../sonar-aligned/types/metrics';
import { MeasuresAndMetaWithMetrics } from '../types/measures';
import { Component, Measure } from '../types/types';
import { StaleTime, createInfiniteQueryHook, createQueryHook } from './common';

const NEW_METRICS = [
  MetricKey.sqale_rating_new,
  MetricKey.security_rating_new,
  MetricKey.reliability_rating_new,
  MetricKey.security_review_rating_new,
  MetricKey.releasability_rating_new,
  MetricKey.new_security_rating_new,
  MetricKey.new_reliability_rating_new,
  MetricKey.new_maintainability_rating_new,
  MetricKey.new_security_review_rating_new,
];

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

export function useComponentMeasuresWithMetricsQuery(
  key: string,
  metricKeys: string[],
  branchParameters: BranchParameters,
  enabled = true,
): UseQueryResult<MeasuresAndMetaWithMetrics> {
  return useQuery({
    enabled,
    queryKey: [
      'component',
      key,
      'measures',
      'with_metrics',
      {
        metricKeys,
        branchParameters,
      },
    ] as const,
    queryFn: ({ queryKey: [, key, , , data] }) => {
      return (
        data &&
        getMeasuresWithMetrics(
          key,
          data.metricKeys.filter((m) => !NEW_METRICS.includes(m as MetricKey)),
          data.branchParameters,
        )
      );
    },
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
          metricKeys: metricKeys
            .split(',')
            .filter((m) => !NEW_METRICS.includes(m as MetricKey))
            .join(),
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

export const useComponentChildrenQuery = createInfiniteQueryHook(
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
    const queryClient = useQueryClient();
    return infiniteQueryOptions({
      queryKey: ['component', component, 'tree', strategy, { metrics, additionalData }],
      queryFn: async ({ pageParam }) => {
        const result = await getComponentTree(
          strategy,
          component,
          metrics?.filter((m) => !NEW_METRICS.includes(m as MetricKey)),
          { ...additionalData, p: pageParam },
        );
        const measuresMapByMetricKeyForBaseComponent = groupBy(
          result.baseComponent.measures,
          'metric',
        );
        metrics?.forEach((metricKey) => {
          const measure = measuresMapByMetricKeyForBaseComponent[metricKey]?.[0] ?? null;
          queryClient.setQueryData<Measure>(
            ['measures', 'details', result.baseComponent.key, metricKey],
            measure,
          );
        });

        result.components.forEach((childComponent) => {
          const measuresMapByMetricKeyForChildComponent = groupBy(
            childComponent.measures,
            'metric',
          );
          metrics?.forEach((metricKey) => {
            const measure = measuresMapByMetricKeyForChildComponent[metricKey]?.[0] ?? null;
            queryClient.setQueryData<Measure>(
              ['measures', 'details', childComponent.key, metricKey],
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

export const useComponentDataQuery = createQueryHook(
  (data: Parameters<typeof getComponentData>[0]) => {
    return queryOptions({
      queryKey: ['component', data.component, 'component_data', omit(data, 'component')],
      queryFn: () => getComponentData(data),
      staleTime: StaleTime.LONG,
    });
  },
);
