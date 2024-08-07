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
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { getMeasures, getMeasuresForProjects } from '../api/measures';
import { getAllTimeMachineData } from '../api/time-machine';
import { Measure } from '../types/types';
import { createQueryHook } from './common';

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

export const useMeasuresForProjectsQuery = createQueryHook(
  ({ projectKeys, metricKeys }: { metricKeys: string[]; projectKeys: string[] }) => {
    const queryClient = useQueryClient();
    return queryOptions({
      queryKey: ['measures', 'list', 'projects', projectKeys, metricKeys],
      queryFn: async () => {
        const measures = await getMeasuresForProjects(projectKeys, metricKeys);
        measures.forEach((measure) => {
          queryClient.setQueryData<Measure>(
            ['measures', 'details', measure.component, measure.metric],
            measure,
          );
        });
        return measures;
      },
    });
  },
);

export const useMeasureQuery = createQueryHook(
  ({ componentKey, metricKey }: { componentKey: string; metricKey: string }) => {
    return queryOptions({
      queryKey: ['measures', 'details', componentKey, metricKey],
      queryFn: () =>
        getMeasures({ component: componentKey, metricKeys: metricKey }).then(
          (measures) => measures[0],
        ),
      staleTime: Infinity,
    });
  },
);
