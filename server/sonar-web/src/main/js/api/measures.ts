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
import { throwGlobalError } from '../helpers/error';
import { getJSON } from '../helpers/request';
import { BranchParameters } from '../types/branch-like';
import {
  MeasuresAndMetaWithMetrics,
  MeasuresAndMetaWithPeriod,
  MeasuresForProjects,
} from '../types/measures';
import { MetricKey, MetricType } from '../types/metrics';
import { Measure } from '../types/types';

const COMPONENT_URL = '/api/measures/component';

export function getMeasures(
  data: { component: string; metricKeys: string } & BranchParameters,
): Promise<Measure[]> {
  return getJSON(COMPONENT_URL, data).then((r) => r.component.measures, throwGlobalError);
}

export async function getMeasuresWithMetrics(
  component: string,
  metrics: string[],
  branchParameters?: BranchParameters,
): Promise<MeasuresAndMetaWithMetrics> {
  // TODO: Remove this mock (SONAR-21259)
  const mockedMetrics = metrics.filter(
    (metric) =>
      metric !== MetricKey.pullrequest_addressed_issues && metric !== MetricKey.new_accepted_issues,
  );
  const result = (await getJSON(COMPONENT_URL, {
    additionalFields: 'metrics',
    component,
    metricKeys: mockedMetrics.join(','),
    ...branchParameters,
  }).catch(throwGlobalError)) as MeasuresAndMetaWithMetrics;
  if (metrics.includes(MetricKey.pullrequest_addressed_issues)) {
    result.metrics.push({
      key: MetricKey.pullrequest_addressed_issues,
      name: 'Addressed Issues',
      description: 'Addressed Issues',
      domain: 'Reliability',
      type: MetricType.Integer,
      higherValuesAreBetter: false,
      qualitative: true,
      hidden: false,
      bestValue: '0',
    });
    result.component.measures?.push({
      metric: MetricKey.pullrequest_addressed_issues,
      period: {
        index: 0,
        value: '11',
      },
    });
  }
  if (metrics.includes(MetricKey.new_accepted_issues)) {
    result.metrics.push({
      key: MetricKey.new_accepted_issues,
      name: 'Accepted Issues',
      description: 'Accepted Issues',
      domain: 'Reliability',
      type: MetricType.Integer,
      higherValuesAreBetter: false,
      qualitative: true,
      hidden: false,
      bestValue: '0',
    });
    result.component.measures?.push({
      metric: MetricKey.new_accepted_issues,
      period: {
        index: 0,
        value: '12',
      },
    });
  }
  return result;
}

export function getMeasuresWithPeriod(
  component: string,
  metrics: string[],
  branchParameters?: BranchParameters,
): Promise<MeasuresAndMetaWithPeriod> {
  return getJSON(COMPONENT_URL, {
    additionalFields: 'period',
    component,
    metricKeys: metrics.join(','),
    ...branchParameters,
  }).catch(throwGlobalError);
}

export function getMeasuresWithPeriodAndMetrics(
  component: string,
  metrics: string[],
  branchParameters?: BranchParameters,
): Promise<MeasuresAndMetaWithPeriod & MeasuresAndMetaWithMetrics> {
  return getJSON(COMPONENT_URL, {
    additionalFields: 'period,metrics',
    component,
    metricKeys: metrics.join(','),
    ...branchParameters,
  }).catch(throwGlobalError);
}

export function getMeasuresForProjects(
  projectKeys: string[],
  metricKeys: string[],
): Promise<MeasuresForProjects[]> {
  return getJSON('/api/measures/search', {
    projectKeys: projectKeys.join(),
    metricKeys: metricKeys.join(),
  }).then((r) => r.measures);
}
