/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface MetricsResponse {
  metrics: T.Metric[];
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
}): Promise<T.Metric[]> {
  return inner(data);

  function inner(
    data: { p?: number; ps?: number } = { ps: 500 },
    prev?: MetricsResponse
  ): Promise<T.Metric[]> {
    return getMetrics(data).then(r => {
      const result = prev ? prev.metrics.concat(r.metrics) : r.metrics;
      if (r.p * r.ps >= r.total) {
        return result;
      }
      return inner({ ...data, p: r.p + 1 }, { ...r, metrics: result });
    });
  }
}

export function getMetricDomains(): Promise<string[]> {
  return getJSON('/api/metrics/domains').then(r => r.domains, throwGlobalError);
}

export function getMetricTypes(): Promise<string[]> {
  return getJSON('/api/metrics/types').then(r => r.types, throwGlobalError);
}

export function createMetric(data: {
  description?: string;
  domain?: string;
  key: string;
  name: string;
  type: string;
}): Promise<T.Metric> {
  return postJSON('/api/metrics/create', data).catch(throwGlobalError);
}

export function updateMetric(data: {
  description?: string;
  domain?: string;
  id: string;
  key?: string;
  name?: string;
  type?: string;
}) {
  return post('/api/metrics/update', data).catch(throwGlobalError);
}

export function deleteMetric(data: { keys: string }) {
  return post('/api/metrics/delete', data).catch(throwGlobalError);
}
