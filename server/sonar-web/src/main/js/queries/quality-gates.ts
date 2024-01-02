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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  copyQualityGate,
  createCondition,
  createQualityGate,
  deleteCondition,
  deleteQualityGate,
  fetchQualityGate,
  fetchQualityGates,
  getGateForProject,
  renameQualityGate,
  setQualityGateAsDefault,
  updateCondition,
} from '../api/quality-gates';
import { getCorrectCaycCondition } from '../apps/quality-gates/utils';
import { addGlobalSuccessMessage } from '../helpers/globalMessages';
import { translate } from '../helpers/l10n';
import { Condition, QualityGate } from '../types/types';

function getQualityGateQueryKey(type: 'name', data: string): string[];
function getQualityGateQueryKey(type: 'project', data: string): string[];
function getQualityGateQueryKey(): string[];
function getQualityGateQueryKey(type?: string, data?: string) {
  return ['quality-gate', type ?? '', data ?? ''].filter(Boolean);
}

export function useQualityGateQuery(name: string) {
  return useQuery({
    queryKey: getQualityGateQueryKey('name', name),
    queryFn: ({ queryKey: [, , name] }) => {
      return fetchQualityGate({ name });
    },
  });
}

export function useComponentQualityGateQuery(project: string) {
  return useQuery({
    queryKey: getQualityGateQueryKey('project', project),
    queryFn: async ({ queryKey: [, , project] }) => {
      const qualityGatePreview = await getGateForProject({ project });
      return fetchQualityGate({ name: qualityGatePreview.name });
    },
  });
}

export function useQualityGatesQuery() {
  return useQuery({
    queryKey: getQualityGateQueryKey(),
    queryFn: () => {
      return fetchQualityGates();
    },
    staleTime: 2 * 60 * 1000,
  });
}

export function useCreateQualityGateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (name: string) => {
      return createQualityGate({ name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
    },
  });
}

export function useSetQualityGateAsDefaultMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (qualityGate: QualityGate) => {
      return setQualityGateAsDefault({ name: qualityGate.name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.invalidateQueries(getQualityGateQueryKey('name', gateName));
    },
  });
}

export function useRenameQualityGateMutation(currentName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newName: string) => {
      return renameQualityGate({ currentName, name: newName });
    },
    onSuccess: (_, newName: string) => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.invalidateQueries(getQualityGateQueryKey('name', newName));
    },
  });
}

export function useCopyQualityGateMutation(sourceName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newName: string) => {
      return copyQualityGate({ sourceName, name: newName });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
    },
  });
}

export function useDeleteQualityGateMutation(name: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => {
      return deleteQualityGate({ name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
    },
  });
}

export function useFixQualityGateMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      weakConditions,
      missingConditions,
    }: {
      weakConditions: Condition[];
      missingConditions: Condition[];
    }) => {
      const promiseArr = weakConditions
        .map((condition) => {
          return updateCondition({
            ...getCorrectCaycCondition(condition),
            id: condition.id,
          });
        })
        .concat(
          missingConditions.map((condition) => {
            return createCondition({
              ...getCorrectCaycCondition(condition),
              gateName,
            });
          }),
        );

      return Promise.all(promiseArr);
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.invalidateQueries(getQualityGateQueryKey('name', gateName));
      addGlobalSuccessMessage(translate('quality_gates.conditions_updated'));
    },
  });
}

export function useCreateConditionMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Omit<Condition, 'id'>) => {
      return createCondition({ ...condition, gateName });
    },
    onSuccess: (_, condition) => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.setQueryData(
        getQualityGateQueryKey('name', gateName),
        (oldData?: QualityGate) => {
          return oldData?.conditions
            ? {
                ...oldData,
                conditions: [...oldData.conditions, condition],
              }
            : undefined;
        },
      );
      queryClient.invalidateQueries(getQualityGateQueryKey('name', gateName));
      addGlobalSuccessMessage(translate('quality_gates.condition_added'));
    },
  });
}

export function useUpdateConditionMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Condition) => {
      return updateCondition(condition);
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.invalidateQueries(getQualityGateQueryKey('name', gateName));
      addGlobalSuccessMessage(translate('quality_gates.condition_updated'));
    },
  });
}

export function useDeleteConditionMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Condition) => {
      return deleteCondition({
        id: condition.id,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(getQualityGateQueryKey());
      queryClient.invalidateQueries(getQualityGateQueryKey('name', gateName));
      addGlobalSuccessMessage(translate('quality_gates.condition_deleted'));
    },
  });
}
