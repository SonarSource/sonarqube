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
import { addGlobalSuccessMessage } from 'design-system';
import { isEqual, keyBy, partition, pick, unionBy } from 'lodash';
import { getActivity } from '../../api/ce';
import {
  addGitlabRolesMapping,
  createGitLabConfiguration,
  deleteGitLabConfiguration,
  deleteGitlabRolesMapping,
  fetchGitLabConfigurations,
  fetchGitlabRolesMapping,
  syncNowGitLabProvisioning,
  updateGitLabConfiguration,
  updateGitlabRolesMapping,
} from '../../api/gitlab-provisioning';
import { translate } from '../../helpers/l10n';
import { mapReactQueryResult } from '../../helpers/react-query';
import { AlmSyncStatus, DevopsRolesMapping, ProvisioningType } from '../../types/provisioning';
import { TaskStatuses, TaskTypes } from '../../types/tasks';
import { createQueryHook, StaleTime } from '../common';

export function useGitLabConfigurationsQuery() {
  return useQuery({
    queryKey: ['identity_provider', 'gitlab_config', 'list'],
    queryFn: fetchGitLabConfigurations,
  });
}

export function useCreateGitLabConfigurationMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof createGitLabConfiguration>[0]) =>
      createGitLabConfiguration(data),
    onSuccess(data) {
      client.setQueryData(['identity_provider', 'gitlab_config', 'list'], {
        gitlabConfigurations: [data],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 1,
        },
      });
    },
  });
}

export function useUpdateGitLabConfigurationMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      data: Parameters<typeof updateGitLabConfiguration>[1];
      id: Parameters<typeof updateGitLabConfiguration>[0];
    }) => updateGitLabConfiguration(id, data),
    onSuccess(data) {
      client.invalidateQueries({ queryKey: ['identity_provider', 'users_and_groups_provider'] });
      client.setQueryData(['identity_provider', 'gitlab_config', 'list'], {
        gitlabConfigurations: [data],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 1,
        },
      });
      addGlobalSuccessMessage(translate('settings.authentication.form.settings.save_success'));
    },
  });
}

export function useDeleteGitLabConfigurationMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: Parameters<typeof deleteGitLabConfiguration>[0]) =>
      deleteGitLabConfiguration(id),
    onSuccess() {
      client.setQueryData(['identity_provider', 'gitlab_config', 'list'], {
        gitlabConfigurations: [],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 0,
        },
      });
    },
  });
}

export function useGilabProvisioningEnabledQuery() {
  const res = useGitLabConfigurationsQuery();

  return mapReactQueryResult(res, (data) =>
    data.gitlabConfigurations?.some(
      (configuration) =>
        configuration.enabled && configuration.provisioningType === ProvisioningType.auto,
    ),
  );
}

export function useGitLabSyncStatusQuery() {
  const getLastSync = async () => {
    const lastSyncTasks = await getActivity({
      type: TaskTypes.GitlabProvisioning,
      p: 1,
      ps: 1,
      status: [TaskStatuses.Success, TaskStatuses.Failed, TaskStatuses.Canceled].join(','),
    });
    const lastSync = lastSyncTasks?.tasks[0];
    if (!lastSync) {
      return undefined;
    }
    const summary = lastSync.infoMessages ? lastSync.infoMessages?.join(', ') : '';
    const errorMessage = lastSync.errorMessage ?? '';
    return {
      executionTimeMs: lastSync?.executionTimeMs ?? 0,
      startedAt: +new Date(lastSync?.startedAt ?? 0),
      finishedAt: +new Date(lastSync?.startedAt ?? 0) + (lastSync?.executionTimeMs ?? 0),
      warningMessage:
        lastSync.warnings && lastSync.warnings.length > 0
          ? lastSync.warnings?.join(', ')
          : undefined,
      status: lastSync?.status as
        | TaskStatuses.Success
        | TaskStatuses.Failed
        | TaskStatuses.Canceled,
      ...(lastSync.status === TaskStatuses.Success ? { summary } : {}),
      ...(lastSync.status !== TaskStatuses.Success ? { errorMessage } : {}),
    };
  };

  const getNextSync = async () => {
    const nextSyncTasks = await getActivity({
      type: TaskTypes.GitlabProvisioning,
      p: 1,
      ps: 1,
      status: [TaskStatuses.Pending, TaskStatuses.InProgress].join(','),
    });
    const nextSync = nextSyncTasks?.tasks[0];
    if (!nextSync) {
      return undefined;
    }
    return { status: nextSync.status as TaskStatuses.Pending | TaskStatuses.InProgress };
  };

  return useQuery({
    queryKey: ['identity_provider', 'gitlab_sync', 'status'],
    queryFn: async () => {
      const [lastSync, nextSync] = await Promise.all([getLastSync(), getNextSync()]);
      return {
        lastSync,
        nextSync,
      } as AlmSyncStatus;
    },
    refetchInterval: 10_000,
  });
}

export function useSyncWithGitLabNow() {
  const queryClient = useQueryClient();
  const { data: syncStatus } = useGitLabSyncStatusQuery();
  const { data: gitlabConfigurations } = useGitLabConfigurationsQuery();
  const autoProvisioningEnabled = gitlabConfigurations?.gitlabConfigurations.some(
    (configuration) =>
      configuration.enabled && configuration.provisioningType === ProvisioningType.auto,
  );
  const mutation = useMutation({
    mutationFn: syncNowGitLabProvisioning,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['identity_provider', 'gitlab_sync'] });
    },
  });

  return {
    synchronizeNow: mutation.mutate,
    canSyncNow: autoProvisioningEnabled && !syncStatus?.nextSync && !mutation.isPending,
  };
}

// Order is reversed to put custom roles at the end (their index is -1)
const defaultRoleOrder = ['owner', 'maintainer', 'developer', 'reporter', 'guest'];

function sortGitlabRoles(data: DevopsRolesMapping[]) {
  return [...data].sort((a, b) => {
    if (defaultRoleOrder.includes(a.id) || defaultRoleOrder.includes(b.id)) {
      return defaultRoleOrder.indexOf(b.id) - defaultRoleOrder.indexOf(a.id);
    }
    return a.role.localeCompare(b.role);
  });
}

export const useGitlabRolesMappingQuery = createQueryHook(() => {
  return queryOptions({
    queryKey: ['identity_provider', 'gitlab_mapping'],
    queryFn: fetchGitlabRolesMapping,
    staleTime: StaleTime.LONG,
    select: sortGitlabRoles,
  });
});

export function useGitlabRolesMappingMutation() {
  const client = useQueryClient();
  const queryKey = ['identity_provider', 'gitlab_mapping'];
  return useMutation({
    mutationFn: async (mapping: DevopsRolesMapping[]) => {
      const state = keyBy(client.getQueryData<DevopsRolesMapping[]>(queryKey), (m) => m.id);

      const [maybeChangedRoles, newRoles] = partition(mapping, (m) => state[m.id]);
      const changedRoles = maybeChangedRoles.filter((item) => !isEqual(item, state[item.id]));
      const deletedRoles = Object.values(state).filter(
        (m) => !m.baseRole && !mapping.some((cm) => m.id === cm.id),
      );

      return {
        addedOrChanged: await Promise.all([
          ...changedRoles.map((data) =>
            updateGitlabRolesMapping(data.id, pick(data, 'permissions')),
          ),
          ...newRoles.map((m) => addGitlabRolesMapping(m)),
        ]),
        deleted: await Promise.all([
          deletedRoles.map((dm) => deleteGitlabRolesMapping(dm.id)),
        ]).then(() => deletedRoles.map((dm) => dm.id)),
      };
    },
    onSuccess: ({ addedOrChanged, deleted }) => {
      const state = client.getQueryData<DevopsRolesMapping[]>(queryKey);
      if (state) {
        const newData = unionBy(
          addedOrChanged,
          state.filter((s) => deleted.find((id) => id === s.id) === undefined),
          (el) => el.id,
        );
        client.setQueryData(queryKey, newData);
      }
      addGlobalSuccessMessage(
        translate('settings.authentication.gitlab.configuration.roles_mapping.save_success'),
      );
    },
  });
}
