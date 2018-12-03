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
import { getJSON, RequestData, postJSON, post } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getMeasures(
  data: { component: string; metricKeys: string } & T.BranchParameters
): Promise<T.Measure[]> {
  return getJSON('/api/measures/component', data).then(r => r.component.measures, throwGlobalError);
}

export function getMeasuresAndMeta(
  component: string,
  metrics: string[],
  additional: RequestData = {}
): Promise<{ component: T.ComponentMeasure; metrics?: T.Metric[]; periods?: T.Period[] }> {
  return getJSON('/api/measures/component', {
    ...additional,
    component,
    metricKeys: metrics.join(',')
  }).catch(throwGlobalError);
}

interface MeasuresForProjects {
  component: string;
  metric: string;
  periods?: T.PeriodMeasure[];
  value?: string;
}

export function getMeasuresForProjects(
  projectKeys: string[],
  metricKeys: string[]
): Promise<MeasuresForProjects[]> {
  return getJSON('/api/measures/search', {
    projectKeys: projectKeys.join(),
    metricKeys: metricKeys.join()
  }).then(r => r.measures);
}

export function getCustomMeasures(data: {
  f?: string;
  p?: number;
  projectKey: string;
  ps?: number;
}): Promise<{ customMeasures: T.CustomMeasure[]; paging: T.Paging }> {
  return getJSON('/api/custom_measures/search', data).then(
    r =>
      ({
        customMeasures: r.customMeasures,
        paging: { pageIndex: r.p, pageSize: r.ps, total: r.total }
      } as any),
    throwGlobalError
  );
}

export function createCustomMeasure(data: {
  description?: string;
  metricKey: string;
  projectKey: string;
  value: string;
}): Promise<T.CustomMeasure> {
  return postJSON('/api/custom_measures/create', data).catch(throwGlobalError);
}

export function updateCustomMeasure(data: { description?: string; id: string; value?: string }) {
  return post('/api/custom_measures/update', data).catch(throwGlobalError);
}

export function deleteCustomMeasure(data: { id: string }) {
  return post('/api/custom_measures/delete', data).catch(throwGlobalError);
}
