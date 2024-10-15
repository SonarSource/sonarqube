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
import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { createRule, deleteRule, getRuleDetails, searchRules, updateRule } from '../api/rules';
import { mapRestRuleToRule } from '../apps/coding-rules/utils';
import { SearchRulesResponse } from '../types/coding-rules';
import { SearchRulesQuery } from '../types/rules';
import { RuleActivation, RuleDetails } from '../types/types';
import { createQueryHook, StaleTime } from './common';

function getRulesQueryKey(type: 'search' | 'details', data?: SearchRulesQuery | string) {
  const key = ['rules', type] as (string | SearchRulesQuery)[];
  if (data) {
    key.push(data);
  }
  return key;
}

export const useSearchRulesQuery = createQueryHook((data: SearchRulesQuery) => {
  return queryOptions({
    queryKey: getRulesQueryKey('search', data),
    queryFn: ({ queryKey: [, , query] }) => {
      if (!query) {
        return null;
      }

      return searchRules(data);
    },
    staleTime: StaleTime.NEVER,
  });
});

export const useRuleDetailsQuery = createQueryHook((data: { actives?: boolean; key: string }) => {
  return queryOptions({
    queryKey: getRulesQueryKey('details', data.key),
    queryFn: () => getRuleDetails(data),
    staleTime: StaleTime.NEVER,
  });
});

export function useCreateRuleMutation(
  searchQuery?: SearchRulesQuery,
  onSuccess?: (rule: RuleDetails) => unknown,
  onError?: (error: Response) => unknown,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createRule,
    onError,
    onSuccess: (rule) => {
      const mappedRule = mapRestRuleToRule(rule);
      onSuccess?.(mappedRule);
      queryClient.setQueryData<SearchRulesResponse>(
        getRulesQueryKey('search', searchQuery),
        (oldData) => {
          return oldData ? { ...oldData, rules: [mappedRule, ...oldData.rules] } : undefined;
        },
      );
    },
  });
}

export function useUpdateRuleMutation(
  searchQuery?: SearchRulesQuery,
  onSuccess?: (rule: RuleDetails) => unknown,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateRule,
    onSuccess: (rule) => {
      onSuccess?.(rule);
      queryClient.setQueryData<SearchRulesResponse>(
        getRulesQueryKey('search', searchQuery),
        (oldData) => {
          return oldData ? { ...oldData, rules: [rule, ...oldData.rules] } : undefined;
        },
      );
      queryClient.setQueryData<{ actives?: RuleActivation[]; rule: RuleDetails }>(
        getRulesQueryKey('details', rule.key),
        (oldData) => {
          return {
            ...oldData,
            rule,
          };
        },
      );
    },
  });
}

export function useDeleteRuleMutation(
  searchQuery?: SearchRulesQuery,
  onSuccess?: (key: string) => unknown,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: { key: string }) => deleteRule(params),
    onSuccess: (_, data) => {
      onSuccess?.(data.key);
      queryClient.setQueryData<SearchRulesResponse>(
        getRulesQueryKey('search', searchQuery),
        (oldData) => {
          return oldData
            ? { ...oldData, rules: oldData.rules.filter((rule) => rule.key !== data.key) }
            : undefined;
        },
      );
    },
  });
}
