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
import { UseQueryResult, useQuery } from '@tanstack/react-query';
import { getTasksForComponent } from '../api/ce';
import { getMeasuresWithMetrics } from '../api/measures';
import { BranchParameters } from '../types/branch-like';
import { MeasuresAndMetaWithMetrics } from '../types/measures';
import { Component } from '../types/types';

const TASK_RETRY = 10_000;

type QueryKeyData = {
  metricKeys: string[];
  branchParameters: BranchParameters;
};

function getComponentQueryKey(key: string, type: 'tasks'): string[];
function getComponentQueryKey(key: string, type: 'measures', data: QueryKeyData): string[];
function getComponentQueryKey(key: string, type: string, data?: QueryKeyData): string[] {
  return ['component', key, type, JSON.stringify(data)];
}

function extractQueryKeyData(queryKey: string[]): { key: string; data?: QueryKeyData } {
  const [, key, , data] = queryKey;
  return { key, data: JSON.parse(data ?? 'null') };
}

export function useTaskForComponentQuery(component: Component) {
  return useQuery({
    queryKey: getComponentQueryKey(component.key, 'tasks'),
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
    queryKey: getComponentQueryKey(key, 'measures', {
      metricKeys,
      branchParameters,
    }),
    queryFn: ({ queryKey }) => {
      const { key, data } = extractQueryKeyData(queryKey);
      return data && getMeasuresWithMetrics(key, data.metricKeys, data.branchParameters);
    },
  });
}
