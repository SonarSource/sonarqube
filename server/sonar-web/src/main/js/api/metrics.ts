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
import { getJSON } from '../helpers/request';
import { Metric } from '../types/types';

export interface MetricsResponse {
  metrics: Metric[];
  p: number;
  ps: number;
  total: number;
}

export function getMetrics(data?: {
  isCustom?: boolean;
  p?: number;
  ps?: number;
}): Promise<MetricsResponse> {
  return getJSON('/api/metrics/search', data).catch(throwGlobalError);
}

export function getAllMetrics(data?: {
  isCustom?: boolean;
  p?: number;
  ps?: number;
}): Promise<Metric[]> {
  return inner(data);

  function inner(
    data: { p?: number; ps?: number } = { ps: 500 },
    prev?: MetricsResponse,
  ): Promise<Metric[]> {
    return getMetrics(data).then((r) => {
      const result = prev ? prev.metrics.concat(r.metrics) : r.metrics;
      if (r.p * r.ps >= r.total) {
        return result;
      }
      return inner({ ...data, p: r.p + 1 }, { ...r, metrics: result });
    });
  }
}
