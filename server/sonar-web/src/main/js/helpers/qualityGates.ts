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

import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import {
  QualityGateApplicationStatusChildProject,
  QualityGateProjectStatus,
  QualityGateStatusCondition,
} from '../types/quality-gates';
import { Metric } from '../types/types';
import { translate } from './l10n';

export function getOperatorLabel(op: string, metric: Metric) {
  return metric.type === MetricType.Rating
    ? translate('quality_gates.operator', op, 'rating')
    : translate('quality_gates.operator', op);
}

export function extractStatusConditionsFromProjectStatus(
  projectStatus: QualityGateProjectStatus,
): QualityGateStatusCondition[] {
  const { conditions } = projectStatus;
  return conditions
    ? conditions.map((c) => ({
        actual: c.actualValue,
        error: c.errorThreshold,
        level: c.status,
        metric: c.metricKey as MetricKey,
        op: c.comparator,
        period: c.periodIndex,
      }))
    : [];
}

export function extractStatusConditionsFromApplicationStatusChildProject(
  projectStatus: QualityGateApplicationStatusChildProject,
): QualityGateStatusCondition[] {
  const { conditions } = projectStatus;
  return conditions
    ? conditions.map((c) => ({
        actual: c.value,
        error: c.errorThreshold,
        level: c.status,
        metric: c.metric as MetricKey,
        op: c.comparator,
        period: c.periodIndex,
      }))
    : [];
}
