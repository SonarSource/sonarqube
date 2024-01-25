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

export function getMeasuresWithMetrics(
  component: string,
  metrics: string[],
  branchParameters?: BranchParameters,
): Promise<MeasuresAndMetaWithMetrics> {
  return getJSON(COMPONENT_URL, {
    additionalFields: 'metrics',
    component,
    metricKeys: metrics.join(','),
    ...branchParameters,
  }).catch(throwGlobalError);
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

export async function getMeasuresWithPeriodAndMetrics(
  component: string,
  metrics: string[],
  branchParameters?: BranchParameters,
): Promise<MeasuresAndMetaWithPeriod & MeasuresAndMetaWithMetrics> {
  // TODO: Remove this mock (SONAR-21488)
  const mockedMetrics = metrics.filter(
    (metric) =>
      ![
        MetricKey.maintainability_issues,
        MetricKey.reliability_issues,
        MetricKey.security_issues,
      ].includes(metric as MetricKey),
  );
  const result = await getJSON(COMPONENT_URL, {
    additionalFields: 'period,metrics',
    component,
    metricKeys: mockedMetrics.join(','),
    ...branchParameters,
  }).catch(throwGlobalError);
  if (metrics.includes(MetricKey.maintainability_issues)) {
    result.metrics.push({
      key: MetricKey.maintainability_issues,
      name: 'Maintainability Issues',
      description: 'Maintainability Issues',
      domain: 'Maintainability',
      type: MetricType.Data,
      higherValuesAreBetter: false,
      qualitative: true,
      hidden: false,
      bestValue: '0',
    });
    result.component.measures?.push({
      metric: MetricKey.maintainability_issues,
      value: JSON.stringify({
        total: 3,
        high: 1,
        medium: 1,
        low: 1,
      }),
    });
  }
  if (metrics.includes(MetricKey.reliability_issues)) {
    result.metrics.push({
      key: MetricKey.reliability_issues,
      name: 'Reliability Issues',
      description: 'Reliability Issues',
      domain: 'Reliability',
      type: MetricType.Data,
      higherValuesAreBetter: false,
      qualitative: true,
      hidden: false,
      bestValue: '0',
    });
    result.component.measures?.push({
      metric: MetricKey.reliability_issues,
      value: JSON.stringify({
        total: 2,
        high: 0,
        medium: 1,
        low: 1,
      }),
    });
  }
  if (metrics.includes(MetricKey.security_issues)) {
    result.metrics.push({
      key: MetricKey.security_issues,
      name: 'Security Issues',
      description: 'Security Issues',
      domain: 'Security',
      type: MetricType.Data,
      higherValuesAreBetter: false,
      qualitative: true,
      hidden: false,
      bestValue: '0',
    });
    result.component.measures?.push({
      metric: MetricKey.security_issues,
      value: JSON.stringify({
        total: 1,
        high: 0,
        medium: 0,
        low: 1,
      }),
    });
  }
  return result;
}

export async function getMeasuresForProjects(
  projectKeys: string[],
  metricKeys: string[],
): Promise<MeasuresForProjects[]> {
  // TODO: Remove this mock (SONAR-21488)
  const mockedMetrics = metricKeys.filter(
    (metric) =>
      ![
        MetricKey.maintainability_issues,
        MetricKey.reliability_issues,
        MetricKey.security_issues,
      ].includes(metric as MetricKey),
  );

  const result = await getJSON('/api/measures/search', {
    projectKeys: projectKeys.join(),
    metricKeys: mockedMetrics.join(),
  }).then((r) => r.measures);

  [
    MetricKey.maintainability_issues,
    MetricKey.reliability_issues,
    MetricKey.security_issues,
  ].forEach((metric) => {
    if (metricKeys.includes(metric)) {
      projectKeys.forEach((projectKey) => {
        result.push({
          component: projectKey,
          metric,
          value: JSON.stringify({
            total: 2,
          }),
        });
      });
    }
  });

  return result;
}
