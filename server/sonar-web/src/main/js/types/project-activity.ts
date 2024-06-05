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
import { Status } from '~sonar-aligned/types/common';
import { MetricKey } from '~sonar-aligned/types/metrics';

interface BaseAnalysis {
  buildString?: string;
  detectedCI?: string;
  events: AnalysisEvent[];
  key: string;
  manualNewCodePeriodBaseline?: boolean;
  projectVersion?: string;
}

export interface Analysis extends BaseAnalysis {
  date: string;
}

export interface ParsedAnalysis extends BaseAnalysis {
  date: Date;
}

export interface AnalysisEvent {
  category: ProjectAnalysisEventCategory | ApplicationAnalysisEventCategory;
  definitionChange?: {
    projects: Array<{
      branch?: string;
      changeType: DefinitionChangeType;
      key: string;
      name: string;
      newBranch?: string;
      oldBranch?: string;
    }>;
  };
  description?: string;
  key: string;
  name: string;
  qualityGate?: {
    failing: Array<{ branch: string; key: string; name: string }>;
    status: Status;
    stillFailing: boolean;
  };
  qualityProfile?: {
    key: string;
    languageKey: string;
    name: string;
  };
}

export enum GraphType {
  issues = 'issues',
  coverage = 'coverage',
  duplications = 'duplications',
  custom = 'custom',
}

export enum ProjectAnalysisEventCategory {
  Version = 'VERSION',
  QualityGate = 'QUALITY_GATE',
  QualityProfile = 'QUALITY_PROFILE',
  SqUpgrade = 'SQ_UPGRADE',
  Other = 'OTHER',
}

export enum ApplicationAnalysisEventCategory {
  QualityGate = 'QUALITY_GATE',
  DefinitionChange = 'DEFINITION_CHANGE',
  Other = 'OTHER',
}

export enum DefinitionChangeType {
  Added = 'ADDED',
  Removed = 'REMOVED',
  BranchChanged = 'BRANCH_CHANGED',
}

export interface HistoryItem {
  date: Date;
  value?: string;
}

export interface MeasureHistory {
  history: HistoryItem[];
  metric: MetricKey;
}

export interface Serie {
  data: Point[];
  name: string;
  translatedName: string;
  type: string;
}

export interface Point {
  x: Date;
  y: number | string | undefined;
}

export type AnalysisMeasuresVariations = Partial<Record<MetricKey, number>>;
