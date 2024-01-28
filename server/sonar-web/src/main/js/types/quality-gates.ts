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
import { BranchLike } from './branch-like';
import { MetricKey } from './metrics';
import { CaycStatus, MeasureEnhanced, Metric, Status } from './types';
import { UserBase } from './users';

export interface QualityGateProjectStatus {
  conditions?: QualityGateProjectStatusCondition[];
  ignoredConditions: boolean;
  status: Status;
  caycStatus: CaycStatus;
}

export interface QualityGateProjectStatusCondition {
  actualValue: string;
  comparator: string;
  errorThreshold: string;
  metricKey: string;
  periodIndex: number;
  status: Status;
}

export interface QualityGateApplicationStatus {
  metrics: Metric[];
  projects: QualityGateApplicationStatusChildProject[];
  status: Status;
}

export interface QualityGateApplicationStatusCondition {
  comparator: string;
  errorThreshold?: string;
  metric: string;
  periodIndex?: number;
  onLeak?: boolean;
  status: Status;
  value: string;
  warningThreshold?: string;
}

export interface QualityGateApplicationStatusChildProject {
  conditions: QualityGateApplicationStatusCondition[];
  key: string;
  name: string;
  status: Status;
  caycStatus: CaycStatus;
}

export interface QualityGateStatus {
  conditions: QualityGateStatusConditionEnhanced[];
  failedConditions: QualityGateStatusConditionEnhanced[];
  ignoredConditions?: boolean;
  caycStatus: CaycStatus;
  key: string;
  name: string;
  status: Status;
  branchLike?: BranchLike;
}

export interface QualityGateStatusCondition {
  actual?: string;
  error?: string;
  level: Status;
  metric: MetricKey;
  op: string;
  period?: number;
  warning?: string;
}

export interface QualityGateStatusConditionEnhanced extends QualityGateStatusCondition {
  measure: MeasureEnhanced;
}

export interface SearchPermissionsParameters {
  gateName: string;
  q?: string;
  selected?: 'all' | 'selected' | 'deselected';
}

export interface AddDeleteUserPermissionsParameters {
  gateName: string;
  login: string;
}

export interface AddDeleteGroupPermissionsParameters {
  gateName: string;
  groupName: string;
}

export interface Group {
  name: string;
}

export function isUser(item: UserBase | Group): item is UserBase {
  return item && (item as UserBase).login !== undefined;
}

export enum QGBadgeType {
  'Missing' = 'missing',
  'Weak' = 'weak',
  'Ok' = 'ok',
}

export enum BadgeTarget {
  QualityGate = 'quality_gate',
  Condition = 'condition',
}
