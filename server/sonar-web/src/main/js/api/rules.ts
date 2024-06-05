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
import { HttpStatusCode } from 'axios';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { axiosToCatch, post, postJSON } from '../helpers/request';
import { CleanCodeAttribute, SoftwareImpact } from '../types/clean-code-taxonomy';
import { GetRulesAppResponse, SearchRulesResponse } from '../types/coding-rules';
import { SearchRulesQuery } from '../types/rules';
import {
  RestRuleDetails,
  RestRuleParameter,
  RuleActivation,
  RuleDetails,
  RulesUpdateRequest,
} from '../types/types';

const RULES_ENDPOINT = '/api/v2/clean-code-policy/rules';

export interface CreateRuleData {
  cleanCodeAttribute: CleanCodeAttribute;
  impacts: SoftwareImpact[];
  key: string;
  markdownDescription: string;
  name: string;
  parameters?: Partial<RestRuleParameter>[];
  status?: string;
  templateKey: string;
}

export function getRulesApp(): Promise<GetRulesAppResponse> {
  return getJSON('/api/rules/app').catch(throwGlobalError);
}

export function searchRules(data: SearchRulesQuery): Promise<SearchRulesResponse> {
  return getJSON('/api/rules/search', data).catch(throwGlobalError);
}

export function listRules(data: SearchRulesQuery): Promise<SearchRulesResponse> {
  return getJSON('/api/rules/list', data).catch(throwGlobalError);
}

export function getRuleRepositories(parameters: {
  q: string;
}): Promise<Array<{ key: string; language: string; name: string }>> {
  return getJSON('/api/rules/repositories', parameters).then(
    ({ repositories }) => repositories,
    throwGlobalError,
  );
}

export function getRuleDetails(parameters: {
  actives?: boolean;
  key: string;
}): Promise<{ actives?: RuleActivation[]; rule: RuleDetails }> {
  return getJSON('/api/rules/show', parameters).catch(throwGlobalError);
}

export function getRuleTags(parameters: { ps?: number; q: string }): Promise<string[]> {
  return getJSON('/api/rules/tags', parameters).then((r) => r.tags, throwGlobalError);
}

export function createRule(data: CreateRuleData): Promise<RestRuleDetails> {
  return axiosToCatch.post<RuleDetails>(RULES_ENDPOINT, data).catch(({ response }) => {
    // do not show global error if the status code is 409
    // this case should be handled inside a component
    if (response && response.status === HttpStatusCode.Conflict) {
      return Promise.reject(response);
    }
    return throwGlobalError(response);
  });
}

export function deleteRule(parameters: { key: string }) {
  return post('/api/rules/delete', parameters).catch(throwGlobalError);
}

export function updateRule(data: RulesUpdateRequest): Promise<RuleDetails> {
  return postJSON('/api/rules/update', data).then((r) => r.rule, throwGlobalError);
}
