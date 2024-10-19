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
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { Paging } from '../types/types';

export interface TimeMachineResponse {
  measures: {
    history: Array<{ date: string; value?: string }>;
    metric: MetricKey;
  }[];
  paging: Paging;
}

export function getTimeMachineData(
  data: {
    component?: string;
    from?: string;
    metrics: string;
    p?: number;
    ps?: number;
    to?: string;
  } & BranchParameters,
): Promise<TimeMachineResponse> {
  return getJSON('/api/measures/search_history', data).catch(throwGlobalError);
}

export function getAllTimeMachineData(
  data: {
    component?: string;
    from?: string;
    metrics: string;
    p?: number;
    to?: string;
  } & BranchParameters,
  prev?: TimeMachineResponse,
): Promise<TimeMachineResponse> {
  return getTimeMachineData({ ...data, ps: 100 }).then((r) => {
    const result = prev
      ? {
          measures: prev.measures.map((measure, idx) => ({
            ...measure,
            history: measure.history.concat(r.measures[idx].history),
          })),
          paging: r.paging,
        }
      : r;

    if (result.paging.pageIndex * result.paging.pageSize >= result.paging.total) {
      return result;
    }
    return getAllTimeMachineData({ ...data, p: result.paging.pageIndex + 1 }, result);
  });
}
