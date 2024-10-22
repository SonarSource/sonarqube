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

export enum RuleStatus {
  Ready = 'READY',
  Beta = 'BETA',
  Deprecated = 'DEPRECATED',
  Removed = 'REMOVED',
}

export interface SearchRulesQuery {
  activation?: boolean | string;
  active_severities?: string;
  asc?: boolean | string;
  available_since?: string;
  cleanCodeAttributeCategories?: string;
  cwe?: string;
  f?: string;
  facets?: string;
  impactSeverities?: string;
  impactSoftwareQualities?: string;
  include_external?: boolean | string;
  inheritance?: string;
  is_template?: boolean | string;
  languages?: string;
  owaspTop10?: string;
  ['owaspTop10-2021']?: string;
  p?: number;
  prioritizedRule?: boolean | string;
  ps?: number;
  q?: string;
  qprofile?: string;
  repositories?: string;
  rule_key?: string;
  s?: string;
  severities?: string;
  sonarsourceSecurity?: string;
  statuses?: string;
  tags?: string;
  template_key?: string;
  types?: string;
}

export enum RulesFacetName {
  AvailableSince = 'availableSince',
  CleanCodeAttributeCategories = 'cleanCodeAttributeCategories',
  Cwe = 'cwe',
  Inheritance = 'inheritance',
  ImpactSoftwareQualities = 'impactSoftwareQualities',
  ImpactSeverities = 'impactSeverities',
  Languages = 'languages',
  Profile = 'profile',
  Repositories = 'repositories',
  Statuses = 'statuses',
  Tags = 'tags',
  Template = 'template',
  Types = 'types',
}
