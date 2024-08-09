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
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { groupBy, isUndefined, omitBy } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { getComponentTree } from '../api/components';
import {
  getMeasures,
  getMeasuresForProjects,
  getMeasuresWithPeriodAndMetrics,
} from '../api/measures';
import { getAllTimeMachineData } from '../api/time-machine';
import { SOFTWARE_QUALITY_RATING_METRICS } from '../helpers/constants';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { getBranchLikeQuery } from '../sonar-aligned/helpers/branch-like';
import { MetricKey } from '../sonar-aligned/types/metrics';
import { BranchLike } from '../types/branch-like';
import { Measure } from '../types/types';
import {createInfiniteQueryHook, createQueryHook} from './common';

const NEW_METRICS = [
  MetricKey.software_quality_maintainability_rating,
  MetricKey.software_quality_security_rating,
  MetricKey.new_software_quality_security_rating,
  MetricKey.software_quality_reliability_rating,
  MetricKey.new_software_quality_reliability_rating,
  MetricKey.software_quality_security_review_rating,
  MetricKey.new_software_quality_security_review_rating,
  MetricKey.new_software_quality_maintainability_rating,
];

export function useAllMeasuresHistoryQuery(
  component: string | undefined,
  branchParams: BranchParameters,
  metrics: string,
  enabled = true,
) {
  return useQuery({
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
    enabled,
  });
}

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
      queryKey: ['measures', 'component', componentKey, 'branchLike', branchLikeQuery, metricKeys],
      queryFn: async () => {
        const data = await getMeasuresWithPeriodAndMetrics(
          componentKey,
          metricKeys.filter((m) => !SOFTWARE_QUALITY_RATING_METRICS.includes(m as MetricKey)),
          branchLikeQuery,
        );
        metricKeys.forEach((metricKey) => {
          const measure =
            data.component.measures?.find((measure) => measure.metric === metricKey) ?? null;
          queryClient.setQueryData<Measure | null>(
            ['measures', 'details', componentKey, 'branchLike', branchLikeQuery, metricKey],
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
      queryKey: ['component', component, 'tree', strategy, { metrics, additionalData }],
      queryFn: async ({ pageParam }) => {
        const result = await getComponentTree(
          strategy,
          component,
          metrics?.filter((m) => !SOFTWARE_QUALITY_RATING_METRICS.includes(m as MetricKey)),
          { ...additionalData, p: pageParam, ...branchLikeQuery },
        );

        // const measuresMapByMetricKeyForBaseComponent = groupBy(
        //   result.baseComponent.measures,
        //   'metric',
        // );
        // metrics?.forEach((metricKey) => {
        //   const measure = measuresMapByMetricKeyForBaseComponent[metricKey]?.[0] ?? null;
        //   queryClient.setQueryData<Measure>(
        //     [
        //       'measures',
        //       'details',
        //       result.baseComponent.key,
        //       'branchLike',
        //       branchLikeQuery,
        //       metricKey,
        //     ],
        //     measure,
        //   );
        // });
        result.components.forEach((childComponent) => {
          const measuresMapByMetricKeyForChildComponent = groupBy(
            childComponent.measures,
            'metric',
          );

          metrics?.forEach((metricKey) => {
            const measure = measuresMapByMetricKeyForChildComponent[metricKey]?.[0] ?? null;
            queryClient.setQueryData<Measure>(
              ['measures', 'details', childComponent.key, 'branchLike', branchLikeQuery, metricKey],
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

export const useMeasuresForProjectsQuery = createQueryHook(
  ({ projectKeys, metricKeys }: { metricKeys: string[]; projectKeys: string[] }) => {
    const queryClient = useQueryClient();

    return queryOptions({
      queryKey: ['measures', 'list', 'projects', projectKeys, metricKeys],
      queryFn: async () => {
        // TODO remove this once all metrics are supported
        const filteredMetricKeys = metricKeys.filter(
          (metricKey) => !SOFTWARE_QUALITY_RATING_METRICS.includes(metricKey as MetricKey),
        );
        const measures = await getMeasuresForProjects(projectKeys, filteredMetricKeys);
        const measuresMapByProjectKey = groupBy(measures, 'component');
        projectKeys.forEach((projectKey) => {
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
    });
  },
);

export const useMeasuresAndLeakQuery = createQueryHook(
  ({
    componentKey,
    metricKeys,
    branchParameters,
  }: {
    branchParameters?: BranchParameters;
    componentKey: string;
    metricKeys: string[];
  }) => {
    const queryClient = useQueryClient();
    return queryOptions({
      queryKey: ['measures', 'details', 'component', componentKey, metricKeys, branchParameters],
      queryFn: async () => {
        // TODO remove this once all metrics are supported
        const filteredMetricKeys = metricKeys.filter(
          (metricKey) => !NEW_METRICS.includes(metricKey as MetricKey),
        );
        const { component, metrics, period } = await getMeasuresWithPeriodAndMetrics(
          componentKey,
          filteredMetricKeys,
          branchParameters,
        );
        const measuresMapByMetricKey = groupBy(component.measures, 'metric');
        metricKeys.forEach((metricKey) => {
          const measure = measuresMapByMetricKey[metricKey]?.[0] ?? null;
          queryClient.setQueryData<Measure>(
            ['measures', 'details', componentKey, metricKey],
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
      queryKey: ['measures', 'details', componentKey, 'branchLike', branchLikeQuery, metricKey],
      queryFn: () =>
        getMeasures({ component: componentKey, metricKeys: metricKey }).then(
          (measures) => measures[0] ?? null,
        ),
      staleTime: Infinity,
    });
  },
);
