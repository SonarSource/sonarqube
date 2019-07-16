/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface GetRulesAppResponse {
  canWrite?: boolean;
  repositories: { key: string; language: string; name: string }[];
}

export function getRulesApp(data: {
  organization: string | undefined;
}): Promise<GetRulesAppResponse> {
  return getJSON('/api/rules/app', data).catch(throwGlobalError);
}

export interface SearchRulesResponse {
  actives?: T.Dict<T.RuleActivation[]>;
  facets?: { property: string; values: { count: number; val: string }[] }[];
  p: number;
  ps: number;
  rules: T.Rule[];
  total: number;
}

export function searchRules(data: {
  organization: string | undefined;
  [x: string]: any;
}): Promise<SearchRulesResponse> {
  return getJSON('/api/rules/search', data).catch(throwGlobalError);
}

export function takeFacet(response: any, property: string) {
  const facet = response.facets.find((facet: any) => facet.property === property);
  return facet ? facet.values : [];
}

export function getRuleDetails(parameters: {
  actives?: boolean;
  key: string;
  organization: string | undefined;
}): Promise<{ actives?: T.RuleActivation[]; rule: T.RuleDetails }> {
  return getJSON('/api/rules/show', parameters).catch(throwGlobalError);
}

export function getRuleTags(parameters: {
  organization: string | undefined;
  ps?: number;
  q: string;
}): Promise<string[]> {
  return getJSON('/api/rules/tags', parameters).then(r => r.tags, throwGlobalError);
}

export function createRule(data: {
  custom_key: string;
  markdown_description: string;
  name: string;
  organization: string | undefined;
  params?: string;
  prevent_reactivation?: boolean;
  severity?: string;
  status?: string;
  template_key: string;
  type?: string;
}): Promise<T.RuleDetails> {
  return postJSON('/api/rules/create', data).then(
    r => r.rule,
    response => {
      // do not show global error if the status code is 409
      // this case should be handled inside a component
      if (response && response.status === 409) {
        return Promise.reject(response);
      } else {
        return throwGlobalError(response);
      }
    }
  );
}

export function deleteRule(parameters: { key: string; organization: string | undefined }) {
  return post('/api/rules/delete', parameters).catch(throwGlobalError);
}

export function updateRule(data: {
  key: string;
  markdown_description?: string;
  markdown_note?: string;
  name?: string;
  organization: string | undefined;
  params?: string;
  remediation_fn_base_effort?: string;
  remediation_fn_type?: string;
  remediation_fy_gap_multiplier?: string;
  severity?: string;
  status?: string;
  tags?: string;
}): Promise<T.RuleDetails> {
  return postJSON('/api/rules/update', data).then(r => r.rule, throwGlobalError);
}
