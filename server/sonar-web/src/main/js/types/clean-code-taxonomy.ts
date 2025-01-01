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

export enum SoftwareImpactSeverity {
  Blocker = 'BLOCKER',
  High = 'HIGH',
  Medium = 'MEDIUM',
  Low = 'LOW',
  Info = 'INFO',
}

export enum CleanCodeAttributeCategory {
  Consistent = 'CONSISTENT',
  Intentional = 'INTENTIONAL',
  Adaptable = 'ADAPTABLE',
  Responsible = 'RESPONSIBLE',
}

export enum CleanCodeAttribute {
  Clear = 'CLEAR',
  Complete = 'COMPLETE',
  Conventional = 'CONVENTIONAL',
  Distinct = 'DISTINCT',
  Efficient = 'EFFICIENT',
  Focused = 'FOCUSED',
  Formatted = 'FORMATTED',
  Identifiable = 'IDENTIFIABLE',
  Lawful = 'LAWFUL',
  Logical = 'LOGICAL',
  Modular = 'MODULAR',
  Respectful = 'RESPECTFUL',
  Tested = 'TESTED',
  Trustworthy = 'TRUSTWORTHY',
}

// The order here is important. Please be mindful about the order when adding new software qualities.
export enum SoftwareQuality {
  Security = 'SECURITY',
  Reliability = 'RELIABILITY',
  Maintainability = 'MAINTAINABILITY',
}

export interface SoftwareImpact {
  severity: SoftwareImpactSeverity;
  softwareQuality: SoftwareQuality;
}

export interface SoftwareImpactMeasureData {
  total: number;
  [SoftwareImpactSeverity.Blocker]: number;
  [SoftwareImpactSeverity.High]: number;
  [SoftwareImpactSeverity.Medium]: number;
  [SoftwareImpactSeverity.Low]: number;
  [SoftwareImpactSeverity.Info]: number;
}
