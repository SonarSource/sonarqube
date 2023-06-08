/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { throwGlobalError } from '../helpers/error';
import { getJSON, post, postJSON } from '../helpers/request';
import { GetRulesAppResponse, SearchRulesResponse } from '../types/coding-rules';
import { SearchRulesQuery } from '../types/rules';
import { RuleActivation, RuleDetails, RulesUpdateRequest } from '../types/types';

export function getRulesApp(organization: string): Promise<GetRulesAppResponse> {
  return getJSON('/api/rules/app', { organization }).catch(throwGlobalError);
}

export function searchRules(data: SearchRulesQuery): Promise<SearchRulesResponse> {
  return getJSON('/api/rules/search', data).catch(throwGlobalError);
}

export function takeFacet(response: SearchRulesResponse, property: string) {
  const facet = response.facets?.find((f) => f.property === property);
  return facet ? facet.values : [];
}

export function getRuleRepositories(parameters: {
  q: string;
}): Promise<Array<{ key: string; language: string; name: string }>> {
  return getJSON('/api/rules/repositories', parameters).then(
    ({ repositories }) => repositories,
    throwGlobalError
  );
}

export function getRuleDetails(parameters: {
  actives?: boolean;
  key: string;
  organization?: string;
}): Promise<{ actives?: RuleActivation[]; rule: RuleDetails }> {
  return getJSON('/api/rules/show', parameters).catch(throwGlobalError);
}

export function getRuleTags(parameters: { organization?: string; ps?: number; q: string }): Promise<string[]> {
  return getJSON('/api/rules/tags', parameters).then((r) => r.tags, throwGlobalError);
}

export function createRule(data: {
  custom_key: string;
  markdown_description: string;
  name: string;
  organization: string;
  params?: string;
  prevent_reactivation?: boolean;
  severity?: string;
  status?: string;
  template_key: string;
  type?: string;
}): Promise<RuleDetails> {
  return postJSON('/api/rules/create', data).then(
    (r) => r.rule,
    (response) => {
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

export function deleteRule(parameters: { key: string; organization?: string }) {
  return post('/api/rules/delete', parameters).catch(throwGlobalError);
}

export function updateRule(data: RulesUpdateRequest): Promise<RuleDetails> {
  return postJSON('/api/rules/update', data).then((r) => r.rule, throwGlobalError);
}
