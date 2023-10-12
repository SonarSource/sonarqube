/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { getLocalizedMetricName } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { MetricKey } from '../../types/metrics';
import { CaycStatus, Condition, Dict, Metric, QualityGate } from '../../types/types';

interface GroupedByMetricConditions {
  overallCodeConditions: Condition[];
  newCodeConditions: Condition[];
  caycConditions: Condition[];
}

type CaycMetricKeys =
  | MetricKey.new_violations
  | MetricKey.new_security_hotspots_reviewed
  | MetricKey.new_coverage
  | MetricKey.new_duplicated_lines_density;

export const CAYC_CONDITIONS: Record<
  CaycMetricKeys,
  Condition & { shouldRenderOperator?: boolean }
> = {
  [MetricKey.new_violations]: {
    id: MetricKey.new_violations,
    metric: MetricKey.new_violations,
    op: 'GT',
    error: '0',
    isCaycCondition: true,
  },
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

const CAYC_CONDITION_ORDER_PRIORITIES: Dict<number> = [
  MetricKey.new_violations,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
]
  .reverse()
  .reduce((acc, key, i) => ({ ...acc, [key.toString()]: i + 1 }), {} as Dict<number>);

const CAYC_CONDITIONS_WITHOUT_FIXED_VALUE: CaycMetricKeys[] = [
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_coverage,
];
const CAYC_CONDITIONS_WITH_FIXED_VALUE: CaycMetricKeys[] = [
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_violations,
];

export function isConditionWithFixedValue(condition: Condition) {
  return CAYC_CONDITIONS_WITH_FIXED_VALUE.includes(condition.metric as CaycMetricKeys);
}

export function getCaycConditionMetadata(condition: Condition) {
  const foundCondition = CAYC_CONDITIONS[condition.metric as CaycMetricKeys];
  return {
    shouldRenderOperator: foundCondition?.shouldRenderOperator,
  };
}

export function getWeakMissingAndNonCaycConditions(conditions: Condition[]) {
  const result: {
    weakConditions: Condition[];
    missingConditions: Condition[];
  } = {
    weakConditions: [],
    missingConditions: [],
  };
  Object.keys(CAYC_CONDITIONS).forEach((key: CaycMetricKeys) => {
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (!selectedCondition) {
      result.missingConditions.push(CAYC_CONDITIONS[key]);
    } else if (
      !CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) &&
      CAYC_CONDITIONS[key]?.error !== selectedCondition.error
    ) {
      result.weakConditions.push(selectedCondition);
    }
  });
  return result;
}

export function getCaycConditionsWithCorrectValue(conditions: Condition[]) {
  return Object.keys(CAYC_CONDITIONS).map((key: CaycMetricKeys) => {
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) && selectedCondition) {
      return selectedCondition;
    }
    return CAYC_CONDITIONS[key];
  });
}

export function groupConditionsByMetric(
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

  function sortFn(a: Condition, b: Condition) {
    const priorityA = CAYC_CONDITION_ORDER_PRIORITIES[a.metric] ?? 0;
    const priorityB = CAYC_CONDITION_ORDER_PRIORITIES[b.metric] ?? 0;
    const diff = priorityB - priorityA;
    if (diff !== 0) {
      return diff;
    }
    return metrics[a.metric].name.localeCompare(metrics[b.metric].name, undefined, {
      sensitivity: 'base',
    });
  }

  groupedConditions.newCodeConditions.sort(sortFn);
  groupedConditions.overallCodeConditions.sort(sortFn);
  groupedConditions.caycConditions.sort(sortFn);

  return groupedConditions;
}

export function getCorrectCaycCondition(condition: Condition) {
  const conditionMetric = condition.metric as CaycMetricKeys;
  if (CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(conditionMetric)) {
    return condition;
  }
  return CAYC_CONDITIONS[conditionMetric];
}

export function addCondition(qualityGate: QualityGate, condition: Condition): QualityGate {
  const oldConditions = qualityGate.conditions || [];
  const conditions = [...oldConditions, condition];
  if (conditions) {
    qualityGate.caycStatus = updateCaycCompliantStatus(conditions);
  }
  return { ...qualityGate, conditions };
}

export function deleteCondition(qualityGate: QualityGate, condition: Condition): QualityGate {
  const conditions =
    qualityGate.conditions && qualityGate.conditions.filter((candidate) => candidate !== condition);
  if (conditions) {
    qualityGate.caycStatus = updateCaycCompliantStatus(conditions);
  }
  return { ...qualityGate, conditions };
}

export function replaceCondition(
  qualityGate: QualityGate,
  newCondition: Condition,
  oldCondition: Condition,
): QualityGate {
  const conditions =
    qualityGate.conditions &&
    qualityGate.conditions.map((candidate) => {
      return candidate === oldCondition ? newCondition : candidate;
    });
  if (conditions) {
    qualityGate.caycStatus = updateCaycCompliantStatus(conditions);
  }

  return { ...qualityGate, conditions };
}

export function updateCaycCompliantStatus(conditions: Condition[]) {
  if (conditions.length < Object.keys(CAYC_CONDITIONS).length) {
    return CaycStatus.NonCompliant;
  }

  for (const key of Object.keys(CAYC_CONDITIONS)) {
    const caycMetric = key as CaycMetricKeys;
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (!selectedCondition) {
      return CaycStatus.NonCompliant;
    }

    if (
      !CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(caycMetric) &&
      selectedCondition &&
      selectedCondition.error !== CAYC_CONDITIONS[caycMetric].error
    ) {
      return CaycStatus.NonCompliant;
    }
  }

  if (conditions.length > Object.keys(CAYC_CONDITIONS).length) {
    return CaycStatus.OverCompliant;
  }

  return CaycStatus.Compliant;
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
  } else if (metric.key === 'new_maintainability_rating') {
    return metrics['sqale_rating'] || metric;
  }
  return metric;
}

export function getLocalizedMetricNameNoDiffMetric(metric: Metric, metrics: Dict<Metric>) {
  return getLocalizedMetricName(getNoDiffMetric(metric, metrics));
}
