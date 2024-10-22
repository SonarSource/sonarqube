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

import { sortBy } from 'lodash';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getLocalizedMetricName } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { CaycStatus, Condition, Dict, Group, Metric, QualityGate } from '../../types/types';
import { UserBase } from '../../types/users';

interface GroupedByMetricConditions {
  caycConditions: Condition[];
  newCodeConditions: Condition[];
  overallCodeConditions: Condition[];
}

type CommonCaycMetricKeys =
  | MetricKey.new_security_hotspots_reviewed
  | MetricKey.new_coverage
  | MetricKey.new_duplicated_lines_density;

type OptimizedCaycMetricKeys = MetricKey.new_violations | CommonCaycMetricKeys;

type UnoptimizedCaycMetricKeys =
  | MetricKey.new_reliability_rating
  | MetricKey.new_security_rating
  | MetricKey.new_maintainability_rating
  | CommonCaycMetricKeys;

type AllCaycMetricKeys = OptimizedCaycMetricKeys | UnoptimizedCaycMetricKeys;

type UserOrGroup = UserBase | Group;
export type QGPermissionOption = UserOrGroup & { label: string; value: string };

const COMMON_CONDITIONS: Record<
  CommonCaycMetricKeys,
  Condition & { shouldRenderOperator?: boolean }
> = {
  [MetricKey.new_security_hotspots_reviewed]: {
    id: MetricKey.new_security_hotspots_reviewed,
    metric: MetricKey.new_security_hotspots_reviewed,
    op: 'LT',
    error: '100',
    isCaycCondition: true,
  },
  [MetricKey.new_coverage]: {
    id: MetricKey.new_coverage,
    metric: MetricKey.new_coverage,
    op: 'LT',
    error: '80',
    isCaycCondition: true,
    shouldRenderOperator: true,
  },
  [MetricKey.new_duplicated_lines_density]: {
    id: MetricKey.new_duplicated_lines_density,
    metric: MetricKey.new_duplicated_lines_density,
    op: 'GT',
    error: '3',
    isCaycCondition: true,
    shouldRenderOperator: true,
  },
};

export const OPTIMIZED_CAYC_CONDITIONS: Record<
  OptimizedCaycMetricKeys,
  Condition & { shouldRenderOperator?: boolean }
> = {
  [MetricKey.new_violations]: {
    id: MetricKey.new_violations,
    metric: MetricKey.new_violations,
    op: 'GT',
    error: '0',
    isCaycCondition: true,
  },
  ...COMMON_CONDITIONS,
};

const UNOPTIMIZED_CAYC_CONDITIONS: Record<
  UnoptimizedCaycMetricKeys,
  Condition & { shouldRenderOperator?: boolean }
> = {
  [MetricKey.new_reliability_rating]: {
    id: MetricKey.new_reliability_rating,
    metric: MetricKey.new_reliability_rating,
    op: 'GT',
    error: '1',
    isCaycCondition: true,
  },
  [MetricKey.new_security_rating]: {
    id: MetricKey.new_security_rating,
    metric: MetricKey.new_security_rating,
    op: 'GT',
    error: '1',
    isCaycCondition: true,
  },
  [MetricKey.new_maintainability_rating]: {
    id: MetricKey.new_maintainability_rating,
    metric: MetricKey.new_maintainability_rating,
    op: 'GT',
    error: '1',
    isCaycCondition: true,
  },
  ...COMMON_CONDITIONS,
};

const ALL_CAYC_CONDITIONS: Record<
  AllCaycMetricKeys,
  Condition & { shouldRenderOperator?: boolean }
> = {
  ...OPTIMIZED_CAYC_CONDITIONS,
  ...UNOPTIMIZED_CAYC_CONDITIONS,
};

export const CAYC_CONDITION_ORDER_PRIORITIES: Dict<number> = {
  [MetricKey.new_violations]: 1,
  [MetricKey.new_security_hotspots_reviewed]: 2,
  [MetricKey.new_coverage]: 3,
  [MetricKey.new_duplicated_lines_density]: 4,
};

const CAYC_CONDITIONS_WITHOUT_FIXED_VALUE: AllCaycMetricKeys[] = [
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_coverage,
];
const CAYC_CONDITIONS_WITH_FIXED_VALUE: AllCaycMetricKeys[] = [
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_violations,
  MetricKey.new_reliability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_maintainability_rating,
];
const NON_EDITABLE_CONDITIONS: MetricKey[] = [MetricKey.prioritized_rule_issues];

export function isConditionWithFixedValue(condition: Condition) {
  return CAYC_CONDITIONS_WITH_FIXED_VALUE.includes(condition.metric as OptimizedCaycMetricKeys);
}

export function isNonEditableMetric(metricKey: MetricKey) {
  return NON_EDITABLE_CONDITIONS.includes(metricKey);
}

export function getCaycConditionMetadata(condition: Condition) {
  const foundCondition = OPTIMIZED_CAYC_CONDITIONS[condition.metric as OptimizedCaycMetricKeys];
  return {
    shouldRenderOperator: foundCondition?.shouldRenderOperator,
  };
}

export function isQualityGateOptimized(qualityGate: QualityGate) {
  return (
    !qualityGate.isBuiltIn &&
    qualityGate.caycStatus !== CaycStatus.NonCompliant &&
    Object.values(OPTIMIZED_CAYC_CONDITIONS).every((condition) => {
      const foundCondition = qualityGate.conditions?.find((c) => c.metric === condition.metric);
      return (
        foundCondition &&
        !isWeakCondition(condition.metric as OptimizedCaycMetricKeys, foundCondition)
      );
    })
  );
}

function isWeakCondition(key: AllCaycMetricKeys, selectedCondition: Condition) {
  return (
    !CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) &&
    ALL_CAYC_CONDITIONS[key]?.error !== selectedCondition.error
  );
}

export function getWeakMissingAndNonCaycConditions(conditions: Condition[]) {
  const result: {
    missingConditions: Condition[];
    weakConditions: Condition[];
  } = {
    weakConditions: [],
    missingConditions: [],
  };
  Object.keys(OPTIMIZED_CAYC_CONDITIONS).forEach((key: OptimizedCaycMetricKeys) => {
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (!selectedCondition) {
      result.missingConditions.push(OPTIMIZED_CAYC_CONDITIONS[key]);
    } else if (isWeakCondition(key, selectedCondition)) {
      result.weakConditions.push(selectedCondition);
    }
  });
  return result;
}

function groupConditionsByMetric(
  conditions: Condition[],
  isBuiltInQG = false,
): GroupedByMetricConditions {
  return conditions.reduce(
    (result, condition) => {
      const isNewCode = isDiffMetric(condition.metric);
      if (condition.isCaycCondition && isBuiltInQG) {
        result.caycConditions.push(condition);
      } else if (isNewCode) {
        result.newCodeConditions.push(condition);
      } else {
        result.overallCodeConditions.push(condition);
      }

      return result;
    },
    {
      overallCodeConditions: [] as Condition[],
      newCodeConditions: [] as Condition[],
      caycConditions: [] as Condition[],
    },
  );
}

export function groupAndSortByPriorityConditions(
  conditions: Condition[],
  metrics: Dict<Metric>,
  isBuiltInQG = false,
): GroupedByMetricConditions {
  const groupedConditions = groupConditionsByMetric(conditions, isBuiltInQG);

  const sortFns = [
    (condition: Condition) => CAYC_CONDITION_ORDER_PRIORITIES[condition.metric],
    (condition: Condition) => metrics[condition.metric]?.name,
  ];

  groupedConditions.newCodeConditions = sortBy(groupedConditions.newCodeConditions, sortFns);
  groupedConditions.overallCodeConditions = sortBy(
    groupedConditions.overallCodeConditions,
    sortFns,
  );
  groupedConditions.caycConditions = sortBy(groupedConditions.caycConditions, sortFns);

  return groupedConditions;
}

export function getCorrectCaycCondition(condition: Condition) {
  const conditionMetric = condition.metric as OptimizedCaycMetricKeys;
  if (CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(conditionMetric)) {
    return condition;
  }
  return OPTIMIZED_CAYC_CONDITIONS[conditionMetric];
}

export function getPossibleOperators(metric: Metric) {
  if (metric.direction === 1) {
    return 'LT';
  } else if (metric.direction === -1) {
    return 'GT';
  }
  return ['LT', 'GT'];
}

function metricKeyExists(key: string, metrics: Dict<Metric>) {
  return metrics[key] !== undefined;
}

function getNoDiffMetric(metric: Metric, metrics: Dict<Metric>) {
  const regularMetricKey = metric.key.replace(/^new_/, '');
  if (isDiffMetric(metric.key) && metricKeyExists(regularMetricKey, metrics)) {
    return metrics[regularMetricKey];
  } else if (metric.key === MetricKey.new_maintainability_rating) {
    return metrics[MetricKey.sqale_rating] || metric;
  }
  return metric;
}

export function getLocalizedMetricNameNoDiffMetric(metric: Metric, metrics: Dict<Metric>) {
  return getLocalizedMetricName(getNoDiffMetric(metric, metrics));
}
