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

export enum ComponentQualifier {
  Application = 'APP',
  Directory = 'DIR',
  Developer = 'DEV',
  File = 'FIL',
  Portfolio = 'VW',
  Project = 'TRK',
  SubPortfolio = 'SVW',
  SubProject = 'BRC',
  TestFile = 'UTS',
}
export interface Breadcrumb {
  key: string;
  name: string;
  qualifier: string;
}
export interface LightComponent {
  organization: string;
  key: string;
  name: string;
  qualifier: string;
}
export enum Visibility {
  Public = 'public',
  Private = 'private',
}
export interface ComponentBase extends LightComponent {
  alm?: { key: string; url: string };
  analysisDate?: string;
  breadcrumbs: Breadcrumb[];
  description?: string;
  isFavorite?: boolean;
  leakPeriodDate?: string;
  path?: string;
  qualityGate?: { isDefault?: boolean; key: string | number; name: string };
  qualityProfiles?: ComponentQualityProfile[];
  refKey?: string;
  tags?: string[];
  version?: string;
  visibility?: Visibility;
}
export interface ComponentQualityProfile {
  deleted?: boolean;
  key: string;
  language: string;
  name: string;
}
