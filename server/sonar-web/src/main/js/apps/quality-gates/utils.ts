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
import { getLocalizedMetricName } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { CaycStatus, Condition, Dict, Metric, QualityGate } from '../../types/types';

const CAYC_CONDITIONS: { [key: string]: Condition } = {
  new_reliability_rating: {
    error: '1',
    id: 'new_reliability_rating',
    metric: 'new_reliability_rating',
    op: 'GT',
  },
  new_security_rating: {
    error: '1',
    id: 'new_security_rating',
    metric: 'new_security_rating',
    op: 'GT',
  },
  new_maintainability_rating: {
    error: '1',
    id: 'new_maintainability_rating',
    metric: 'new_maintainability_rating',
    op: 'GT',
  },
  new_security_hotspots_reviewed: {
    error: '100',
    id: 'new_security_hotspots_reviewed',
    metric: 'new_security_hotspots_reviewed',
    op: 'LT',
  },
  new_coverage: {
    id: 'AXJMbIUHPAOIsUIE3eOF',
    metric: 'new_coverage',
    op: 'LT',
    error: '80',
  },
  new_duplicated_lines_density: {
    id: 'AXJMbIUHPAOIsUIE3eOG',
    metric: 'new_duplicated_lines_density',
    op: 'GT',
    error: '3',
  },
};

export const CAYC_CONDITIONS_WITHOUT_FIXED_VALUE = ['new_duplicated_lines_density', 'new_coverage'];
export const CAYC_CONDITIONS_WITH_FIXED_VALUE = [
  'new_security_hotspots_reviewed',
  'new_maintainability_rating',
  'new_security_rating',
  'new_reliability_rating',
];

export function isCaycCondition(condition: Condition) {
  return condition.metric in CAYC_CONDITIONS;
}

export function getWeakMissingAndNonCaycConditions(conditions: Condition[]) {
  const result: {
    weakConditions: Condition[];
    missingConditions: Condition[];
  } = {
    weakConditions: [],
    missingConditions: [],
  };
  Object.keys(CAYC_CONDITIONS).forEach((key) => {
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (!selectedCondition) {
      result.missingConditions.push(CAYC_CONDITIONS[key]);
    } else if (
      !CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) &&
      CAYC_CONDITIONS[key].error !== selectedCondition.error
    ) {
      result.weakConditions.push(selectedCondition);
    }
  });
  return result;
}

export function getCaycConditionsWithCorrectValue(conditions: Condition[]) {
  return Object.keys(CAYC_CONDITIONS).map((key) => {
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) && selectedCondition) {
      return selectedCondition;
    }
    return CAYC_CONDITIONS[key];
  });
}

export function getCorrectCaycCondition(condition: Condition) {
  if (CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(condition.metric)) {
    return condition;
  }
  return CAYC_CONDITIONS[condition.metric];
}

export function checkIfDefault(qualityGate: QualityGate, list: QualityGate[]): boolean {
  const finding = list.find((candidate) => candidate.id === qualityGate.id);
  return (finding && finding.isDefault) || false;
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
  oldCondition: Condition
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
    const selectedCondition = conditions.find((condition) => condition.metric === key);
    if (!selectedCondition) {
      return CaycStatus.NonCompliant;
    }

    if (
      !CAYC_CONDITIONS_WITHOUT_FIXED_VALUE.includes(key) &&
      selectedCondition &&
      selectedCondition.error !== CAYC_CONDITIONS[key].error
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
  return metrics && metrics[key] !== undefined;
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
