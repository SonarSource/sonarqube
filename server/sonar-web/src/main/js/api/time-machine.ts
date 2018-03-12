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
import { Paging, BranchParameters } from '../app/types';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface HistoryItem {
  date: Date;
  value: string;
}

export interface History {
  [metric: string]: HistoryItem[];
}

interface TimeMachineResponse {
  measures: {
    metric: string;
    history: HistoryItem[];
  }[];
  paging: Paging;
}

export function getTimeMachineData(
  data: {
    component: string;
    from?: string;
    metrics: string;
    p?: number;
    ps?: number;
    to?: string;
  } & BranchParameters
): Promise<TimeMachineResponse> {
  return getJSON('/api/measures/search_history', data).catch(throwGlobalError);
}

export function getAllTimeMachineData(
  data: {
    component: string;
    metrics: string;
    from?: string;
    p?: number;
    to?: string;
  } & BranchParameters,
  prev?: TimeMachineResponse
): Promise<TimeMachineResponse> {
  return getTimeMachineData({ ...data, ps: 1000 }).then(r => {
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
    return getAllTimeMachineData({ ...data, p: result.paging.pageIndex + 1 }, result);
  });
}
