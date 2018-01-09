/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getJSON } from '../helpers/request';

interface TimeMachineResponse {
  measures: Array<{
    metric: string;
    history: Array<{ date: string; value: string }>;
  }>;
  paging: { pageIndex: number; pageSize: number; total: number };
}

export function getTimeMachineData(
  component: string,
  metrics: string[],
  other?: { p?: number; ps?: number; from?: string; to?: string }
): Promise<TimeMachineResponse> {
  return getJSON('/api/measures/search_history', {
    component,
    metrics: metrics.join(),
    ps: 1000,
    ...other
  });
}

export function getAllTimeMachineData(
  component: string,
  metrics: Array<string>,
  other?: { p?: number; from?: string; to?: string },
  prev?: TimeMachineResponse
): Promise<TimeMachineResponse> {
  return getTimeMachineData(component, metrics, { ...other, ps: 1000 }).then(r => {
    const result = prev
      ? {
          measures: prev.measures.map((measure, idx) => ({
            ...measure,
            history: measure.history.concat(r.measures[idx].history)
          })),
          paging: r.paging
        }
      : r;

    if (result.paging.pageIndex * result.paging.pageSize >= result.paging.total) {
      return result;
    }
    return getAllTimeMachineData(
      component,
      metrics,
      { ...other, p: result.paging.pageIndex + 1 },
      result
    );
  });
}
