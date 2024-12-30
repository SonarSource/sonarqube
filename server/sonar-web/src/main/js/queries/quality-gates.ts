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

import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useIntl } from 'react-intl';
import { addGlobalSuccessMessage } from '~design-system';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import {
  associateGateWithProject,
  copyQualityGate,
  createCondition,
  createQualityGate,
  deleteCondition,
  deleteQualityGate,
  dissociateGateWithProject,
  fetchQualityGate,
  fetchQualityGates,
  getAllQualityGateProjects,
  getApplicationQualityGate,
  getGateForProject,
  getQualityGateProjectStatus,
  renameQualityGate,
  setQualityGateAiQualified,
  setQualityGateAsDefault,
  updateCondition,
} from '../api/quality-gates';
import { getCorrectCaycCondition } from '../apps/quality-gates/utils';
import { translate } from '../helpers/l10n';
import { Condition, QualityGate } from '../types/types';
import { createQueryHook, StaleTime } from './common';

const QUERY_STALE_TIME = 5 * 60 * 1000;

const qualityQuery = {
  all: () => ['quality-gate'] as const,
  list: (organization) => [...qualityQuery.all(), 'list', organization] as const,
  details: () => [...qualityQuery.all(), 'details'] as const,
  detail: (name?: string) => [...qualityQuery.details(), name ?? ''] as const,
  projectsAssoc: () => [...qualityQuery.all(), 'project-assoc'] as const,
  projectAssoc: (project: string) => [...qualityQuery.projectsAssoc(), project] as const,
  allProjectsSearch: (qualityGate: string) =>
    [...qualityQuery.all(), 'all-project-search', qualityGate] as const,
};

// This is internal to "enable" query when searching from the project page
function useQualityGateQueryInner(organization: string, name?: string) {
  return useQuery({
    queryKey: qualityQuery.detail(name),
    queryFn: ({ queryKey: [, , name] }) => {
      return fetchQualityGate({ organization, name });
    },
    enabled: name !== undefined,
    staleTime: QUERY_STALE_TIME,
  });
}

export function useQualityGateQuery(organization: string, name: string) {
  return useQualityGateQueryInner(organization, name);
}

export function useQualityGateForProjectQuery(organization: string, project: string) {
  return useQuery({
    queryKey: qualityQuery.projectAssoc(project),
    queryFn: async ({ queryKey: [, , project] }) => {
      const qualityGatePreview = await getGateForProject({ organization, project });
      return qualityGatePreview.name;
    },
  });
}

export function useComponentQualityGateQuery(organization: string, project: string) {
  const { data: name } = useQualityGateForProjectQuery(organization, project);
  return useQualityGateQueryInner(organization, name);
}

export const useQualityGatesQuery = createQueryHook((organization: string) => {
  return queryOptions({
    queryKey: qualityQuery.list(organization),
    queryFn: () => {
      return fetchQualityGates({ organization });
    },
    staleTime: StaleTime.LONG,
  });
});

export const useGetAllQualityGateProjectsQuery = createQueryHook(
  (data: Parameters<typeof getAllQualityGateProjects>[0]) => {
    return queryOptions({
      queryKey: qualityQuery.allProjectsSearch(data?.gateName ?? ''),
      queryFn: () => {
        return getAllQualityGateProjects(data);
      },
    });
  },
);

export function useCreateQualityGateMutation(organization: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (name: string) => {
      return createQualityGate({ organization, name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
    },
  });
}

export function useSetQualityGateAsDefaultMutation(organization: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (qualityGate: QualityGate) => {
      return setQualityGateAsDefault({ name: qualityGate.name, organization });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.details() });
    },
  });
}

export function useRenameQualityGateMutation(organization: string, currentName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newName: string) => {
      return renameQualityGate({ organization, currentName, name: newName });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.projectsAssoc() });
      queryClient.removeQueries({ queryKey: qualityQuery.detail(currentName) });
    },
  });
}

export function useCopyQualityGateMutation(organization: string, sourceName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newName: string) => {
      return copyQualityGate({ organization, sourceName, name: newName });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
    },
  });
}

export function useDeleteQualityGateMutation(organization: string, name: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => {
      return deleteQualityGate({ organization, name });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.projectsAssoc() });
      queryClient.removeQueries({ queryKey: qualityQuery.detail(name) });
    },
  });
}

export function useSetAiSupportedQualityGateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      name,
      isQualityGateAiSupported,
    }: {
      isQualityGateAiSupported: boolean;
      name: string;
    }) => {
      return setQualityGateAiQualified(name, isQualityGateAiSupported);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.all() });
    },
  });
}

export function useAssociateGateWithProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { gateName: string; projectKey: string }) => {
      return associateGateWithProject(data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.projectsAssoc() });
      queryClient.invalidateQueries({ queryKey: ['project', 'list'] });
    },
  });
}

export function useDissociateGateWithProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { projectKey: string }) => {
      return dissociateGateWithProject(data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.projectsAssoc() });
      queryClient.invalidateQueries({ queryKey: ['project', 'list'] });
    },
  });
}

export function useFixQualityGateMutation(organization: string, gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      weakConditions,
      missingConditions,
    }: {
      missingConditions: Condition[];
      weakConditions: Condition[];
    }) => {
      const promiseArr = weakConditions
        .map((condition) => {
          return updateCondition({
            organization,
            ...getCorrectCaycCondition(condition),
            id: condition.id,
          });
        })
        .concat(
          missingConditions.map((condition) => {
            return createCondition({
              organization,
              ...getCorrectCaycCondition(condition),
              gateName,
            });
          }),
        );

      return Promise.all(promiseArr);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      addGlobalSuccessMessage(translate('quality_gates.conditions_updated'));
    },
  });
}

export function useCreateConditionMutation(organization: string, gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Omit<Condition, 'id'>) => {
      return createCondition({ organization, ...condition, gateName });
    },
    onSuccess: (_, condition) => {
      queryClient.setQueryData(qualityQuery.detail(gateName), (oldData?: QualityGate) => {
        return oldData?.conditions
          ? {
              ...oldData,
              conditions: [...oldData.conditions, condition],
            }
          : undefined;
      });
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      addGlobalSuccessMessage(translate('quality_gates.condition_added'));
    },
  });
}

export function useUpdateConditionMutation(organization: string, gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Condition) => {
      return updateCondition({ organization, ...condition });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
      addGlobalSuccessMessage(translate('quality_gates.condition_updated'));
    },
  });
}

export function useUpdateOrDeleteConditionsMutation(organization: string, gateName: string, isSingleMetric = false) {
  const queryClient = useQueryClient();
  const intl = useIntl();

  return useMutation({
    mutationFn: (
      conditions: (Omit<Condition, 'metric'> & { metric: string | null | undefined })[],
    ) => {
      const promiseArr = conditions.map((condition) =>
        condition.metric
          ? updateCondition(condition as Condition)
          : deleteCondition({ id: condition.id }),
      );

      return Promise.all(promiseArr);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list() });
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
      addGlobalSuccessMessage(
        intl.formatMessage(
          {
            id: isSingleMetric
              ? 'quality_gates.condition_updated'
              : 'quality_gates.conditions_updated_to_the_mode',
          },
          { qualityGateName: gateName },
        ),
      );
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
    },
  });
}

export function useDeleteConditionMutation(gateName: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (condition: Condition) => {
      return deleteCondition({
        organization,
        id: condition.id,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qualityQuery.list(organization) });
      queryClient.invalidateQueries({ queryKey: qualityQuery.detail(gateName) });
      addGlobalSuccessMessage(translate('quality_gates.condition_deleted'));
    },
  });
}

export const useProjectQualityGateStatus = createQueryHook(
  ({
    projectId,
    projectKey,
    branchParameters,
  }: {
    branchParameters?: BranchParameters;
    projectId?: string;
    projectKey?: string;
  }) => {
    return queryOptions({
      queryKey: ['quality-gate', 'status', 'project', projectId, projectKey, branchParameters],
      queryFn: () => getQualityGateProjectStatus({ projectId, projectKey, ...branchParameters }),
    });
  },
);

export const useApplicationQualityGateStatus = createQueryHook(
  ({ application, branch }: { application: string; branch?: string }) => {
    return queryOptions({
      queryKey: ['quality-gate', 'status', 'application', application, branch],
      queryFn: () => getApplicationQualityGate({ application, branch }),
    });
  },
);

/**
 * @deprecated This is only for component that has not been migrated to react-query
 */
export function useInvalidateQualityGateQuery() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: qualityQuery.all() });
  };
}
