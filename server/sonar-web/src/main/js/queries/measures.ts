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

import { useQuery } from '@tanstack/react-query';
import { getAllTimeMachineData } from '../api/time-machine';
import { BranchParameters } from '../types/branch-like';

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
