/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { RuleInheritance } from '../../app/types';
import {
  RawQuery,
  parseAsString,
  parseAsArray,
  serializeString,
  serializeStringArray,
  cleanQuery,
  queriesEqual,
  parseAsDate,
  serializeDateShort,
  parseAsOptionalBoolean,
  serializeOptionalBoolean,
  parseAsOptionalString
} from '../../helpers/query';

export interface Query {
  activation: boolean | undefined;
  activationSeverities: string[];
  availableSince: Date | undefined;
  compareToProfile: string | undefined;
  inheritance: RuleInheritance | undefined;
  languages: string[];
  profile: string | undefined;
  repositories: string[];
  ruleKey: string | undefined;
  searchQuery: string | undefined;
  severities: string[];
  statuses: string[];
  tags: string[];
  template: boolean | undefined;
  types: string[];
}

export type FacetKey = keyof Query;

export interface Facet {
  [value: string]: number;
}

export type Facets = { [F in FacetKey]?: Facet };

export type OpenFacets = { [F in FacetKey]?: boolean };

export interface Activation {
  inherit: string;
  severity: string;
}

export interface Actives {
  [rule: string]: {
    [profile: string]: Activation;
  };
}

export function parseQuery(query: RawQuery): Query {
  return {
    activation: parseAsOptionalBoolean(query.activation),
    activationSeverities: parseAsArray(query.active_severities, parseAsString),
    availableSince: parseAsDate(query.available_since),
    compareToProfile: parseAsOptionalString(query.compareToProfile),
    inheritance: parseAsInheritance(query.inheritance),
    languages: parseAsArray(query.languages, parseAsString),
    profile: parseAsOptionalString(query.qprofile),
    repositories: parseAsArray(query.repositories, parseAsString),
    ruleKey: parseAsOptionalString(query.rule_key),
    searchQuery: parseAsOptionalString(query.q),
    severities: parseAsArray(query.severities, parseAsString),
    statuses: parseAsArray(query.statuses, parseAsString),
    tags: parseAsArray(query.tags, parseAsString),
    template: parseAsOptionalBoolean(query.is_template),
    types: parseAsArray(query.types, parseAsString)
  };
}

export function serializeQuery(query: Query): RawQuery {
  /* eslint-disable camelcase */
  return cleanQuery({
    activation: serializeOptionalBoolean(query.activation),
    active_severities: serializeStringArray(query.activationSeverities),
    available_since: serializeDateShort(query.availableSince),
    compareToProfile: serializeString(query.compareToProfile),
    inheritance: serializeInheritance(query.inheritance),
    is_template: serializeOptionalBoolean(query.template),
    languages: serializeStringArray(query.languages),
    q: serializeString(query.searchQuery),
    qprofile: serializeString(query.profile),
    repositories: serializeStringArray(query.repositories),
    rule_key: serializeString(query.ruleKey),
    severities: serializeStringArray(query.severities),
    statuses: serializeStringArray(query.statuses),
    tags: serializeStringArray(query.tags),
    types: serializeStringArray(query.types)
  });
  /* eslint-enable camelcase */
}

export function areQueriesEqual(a: RawQuery, b: RawQuery) {
  return queriesEqual(parseQuery(a), parseQuery(b));
}

export function shouldRequestFacet(facet: FacetKey) {
  const facetsToRequest = [
    'activationSeverities',
    'languages',
    'repositories',
    'severities',
    'statuses',
    'tags',
    'types'
  ];
  return facetsToRequest.includes(facet);
}

export function getServerFacet(facet: FacetKey) {
  return facet === 'activationSeverities' ? 'active_severities' : facet;
}

export function getAppFacet(serverFacet: string): FacetKey {
  return serverFacet === 'active_severities' ? 'activationSeverities' : (serverFacet as FacetKey);
}

export function getOpen(query: RawQuery) {
  return query.open;
}

function parseAsInheritance(value?: string): RuleInheritance | undefined {
  if (value === RuleInheritance.Inherited) {
    return RuleInheritance.Inherited;
  } else if (value === RuleInheritance.NotInherited) {
    return RuleInheritance.NotInherited;
  } else if (value === RuleInheritance.Overridden) {
    return RuleInheritance.Overridden;
  } else {
    return undefined;
  }
}

function serializeInheritance(value: RuleInheritance | undefined): string | undefined {
  return value;
}
