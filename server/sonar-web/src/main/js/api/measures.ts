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
import { getJSON, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getMeasures(
  componentKey: string,
  metrics: string[],
  branch?: string
): Promise<Array<{ metric: string; value?: string }>> {
  const url = '/api/measures/component';
  const data = { componentKey, metricKeys: metrics.join(','), branch };
  return getJSON(url, data).then(r => r.component.measures, throwGlobalError);
}

export function getMeasuresAndMeta(
  componentKey: string,
  metrics: string[],
  additional: RequestData = {}
): Promise<any> {
  const data = { ...additional, componentKey, metricKeys: metrics.join(',') };
  return getJSON('/api/measures/component', data);
}

export interface Period {
  index: number;
  value: string;
}

export interface Measure {
  component: string;
  metric: string;
  periods?: Period[];
  value?: string;
}

export function getMeasuresForProjects(
  projectKeys: string[],
  metricKeys: string[]
): Promise<Measure[]> {
  return getJSON('/api/measures/search', {
    projectKeys: projectKeys.join(),
    metricKeys: metricKeys.join()
  }).then(r => r.measures);
}
