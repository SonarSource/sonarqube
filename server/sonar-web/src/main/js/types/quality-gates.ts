/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

export interface QualityGateProjectStatus {
  conditions?: QualityGateProjectStatusCondition[];
  ignoredConditions: boolean;
  status: T.Status;
}

export interface QualityGateProjectStatusCondition {
  actualValue: string;
  comparator: string;
  errorThreshold: string;
  metricKey: string;
  periodIndex: number;
  status: T.Status;
}

export interface QualityGateApplicationStatus {
  metrics: T.Metric[];
  projects: QualityGateApplicationStatusChildProject[];
  status: T.Status;
}

export interface QualityGateApplicationStatusCondition {
  comparator: string;
  errorThreshold?: string;
  metric: string;
  periodIndex?: number;
  onLeak?: boolean;
  status: T.Status;
  value: string;
  warningThreshold?: string;
}

export interface QualityGateApplicationStatusChildProject {
  conditions: QualityGateApplicationStatusCondition[];
  key: string;
  name: string;
  status: T.Status;
}

export interface QualityGateStatus {
  failedConditions: QualityGateStatusConditionEnhanced[];
  ignoredConditions?: boolean;
  key: string;
  name: string;
  status: T.Status;
  branchLike?: BranchLike;
}

export interface QualityGateStatusCondition {
  actual?: string;
  error?: string;
  level: T.Status;
  metric: string;
  op: string;
  period?: number;
  warning?: string;
}

export interface QualityGateStatusConditionEnhanced extends QualityGateStatusCondition {
  measure: T.MeasureEnhanced;
}

export interface SearchPermissionsParameters {
  qualityGate: string;
  q?: string;
  selected?: 'all' | 'selected' | 'deselected';
}
