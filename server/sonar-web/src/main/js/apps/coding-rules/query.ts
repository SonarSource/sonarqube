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
import { RawQuery } from '~sonar-aligned/types/router';
import {
  cleanQuery,
  parseAsArray,
  parseAsDate,
  parseAsOptionalBoolean,
  parseAsOptionalString,
  parseAsString,
  queriesEqual,
  serializeDateShort,
  serializeOptionalBoolean,
  serializeString,
  serializeStringArray,
} from '../../helpers/query';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { Dict, RuleActivation, RuleInheritance } from '../../types/types';

export interface Query {
  activation: boolean | undefined;
  availableSince: Date | undefined;
  cleanCodeAttributeCategories: CleanCodeAttributeCategory[];
  compareToProfile: string | undefined;
  cwe: string[];
  impactSeverities: SoftwareImpactSeverity[];
  impactSoftwareQualities: SoftwareQuality[];
  inheritance: RuleInheritance | undefined;
  languages: string[];
  owaspTop10: string[];
  'owaspTop10-2021': string[];
  profile: string | undefined;
  repositories: string[];
  ruleKey: string | undefined;
  searchQuery: string | undefined;
  severities: string[];
  sonarsourceSecurity: string[];
  statuses: string[];
  tags: string[];
  template: boolean | undefined;
  types: string[];
  prioritizedRule: boolean | undefined;
}

export type FacetKey = keyof Query;

export interface Facet {
  [value: string]: number;
}

export type Facets = { [F in FacetKey]?: Facet };

export type OpenFacets = Dict<boolean>;

export interface Actives {
  [rule: string]: {
    [profile: string]: RuleActivation;
  };
}

export function parseQuery(query: RawQuery): Query {
  return {
    activation: parseAsOptionalBoolean(query.activation),
    availableSince: parseAsDate(query.available_since),
    cleanCodeAttributeCategories: parseAsArray<CleanCodeAttributeCategory>(
      query.cleanCodeAttributeCategories,
      parseAsString,
    ),
    compareToProfile: parseAsOptionalString(query.compareToProfile),
    cwe: parseAsArray(query.cwe, parseAsString),
    impactSeverities: parseAsArray<SoftwareImpactSeverity>(query.impactSeverities, parseAsString),
    impactSoftwareQualities: parseAsArray<SoftwareQuality>(
      query.impactSoftwareQualities,
      parseAsString,
    ),
    inheritance: parseAsInheritance(query.inheritance),
    languages: parseAsArray(query.languages, parseAsString),
    owaspTop10: parseAsArray(query.owaspTop10, parseAsString),
    'owaspTop10-2021': parseAsArray(query['owaspTop10-2021'], parseAsString),
    profile: parseAsOptionalString(query.qprofile),
    repositories: parseAsArray(query.repositories, parseAsString),
    ruleKey: parseAsOptionalString(query.rule_key),
    searchQuery: parseAsOptionalString(query.q),
    severities: parseAsArray(query.severities, parseAsString),
    sonarsourceSecurity: parseAsArray(query.sonarsourceSecurity, parseAsString),
    statuses: parseAsArray(query.statuses, parseAsString),
    tags: parseAsArray(query.tags, parseAsString),
    template: parseAsOptionalBoolean(query.is_template),
    types: parseAsArray(query.types, parseAsString),
    prioritizedRule: parseAsOptionalBoolean(query.prioritizedRule),
  };
}

export function serializeQuery(query: Query): RawQuery {
  return cleanQuery({
    activation: serializeOptionalBoolean(query.activation),
    available_since: serializeDateShort(query.availableSince),
    cleanCodeAttributeCategories: serializeStringArray(query.cleanCodeAttributeCategories),
    compareToProfile: serializeString(query.compareToProfile),
    cwe: serializeStringArray(query.cwe),
    inheritance: serializeInheritance(query.inheritance),
    impactSeverities: serializeStringArray(query.impactSeverities),
    impactSoftwareQualities: serializeStringArray(query.impactSoftwareQualities),
    is_template: serializeOptionalBoolean(query.template),
    languages: serializeStringArray(query.languages),
    owaspTop10: serializeStringArray(query.owaspTop10),
    'owaspTop10-2021': serializeStringArray(query['owaspTop10-2021']),
    q: serializeString(query.searchQuery),
    qprofile: serializeString(query.profile),
    repositories: serializeStringArray(query.repositories),
    rule_key: serializeString(query.ruleKey),
    severities: serializeStringArray(query.severities),
    sonarsourceSecurity: serializeStringArray(query.sonarsourceSecurity),
    statuses: serializeStringArray(query.statuses),
    tags: serializeStringArray(query.tags),
    types: serializeStringArray(query.types),
    prioritizedRule: serializeOptionalBoolean(query.prioritizedRule),
  });
}

export function areQueriesEqual(a: RawQuery, b: RawQuery) {
  return queriesEqual(parseQuery(a), parseQuery(b));
}

export function shouldRequestFacet(facet: string): facet is FacetKey {
  const facetsToRequest = [
    'activationSeverities',
    'cwe',
    'languages',
    'owaspTop10',
    'owaspTop10-2021',
    'repositories',
    'severities',
    'sonarsourceSecurity',
    'standard',
    'statuses',
    'tags',
    'types',
    'cleanCodeAttributeCategories',
    'impactSoftwareQualities',
    'impactSeverities',
  ];
  return facetsToRequest.includes(facet);
}

export function getOpen(query: RawQuery) {
  return query.open;
}

export function getSelected(query: RawQuery) {
  return query.selected;
}

export function hasRuleKey(query: RawQuery) {
  return Boolean(query.rule_key);
}

function parseAsInheritance(value?: string): RuleInheritance | undefined {
  if (value === 'INHERITED' || value === 'NONE' || value === 'OVERRIDES') {
    return value;
  }
  return undefined;
}

function serializeInheritance(value: RuleInheritance | undefined): string | undefined {
  return value;
}
