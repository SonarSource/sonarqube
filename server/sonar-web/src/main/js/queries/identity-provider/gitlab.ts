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
import { addGlobalSuccessMessage } from 'design-system';
import { getActivity } from '../../api/ce';
import {
  createGitLabConfiguration,
  deleteGitLabConfiguration,
  fetchGitLabConfigurations,
  syncNowGitLabProvisioning,
  updateGitLabConfiguration,
} from '../../api/gitlab-provisioning';
import { translate } from '../../helpers/l10n';
import { AlmSyncStatus, ProvisioningType } from '../../types/provisioning';
import { TaskStatuses, TaskTypes } from '../../types/tasks';

export function useGitLabConfigurationsQuery() {
  return useQuery(['identity_provider', 'gitlab_config', 'list'], fetchGitLabConfigurations);
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
      id: Parameters<typeof updateGitLabConfiguration>[0];
      data: Parameters<typeof updateGitLabConfiguration>[1];
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

  return useQuery(
    ['identity_provider', 'gitlab_sync', 'status'],
    async () => {
      const [lastSync, nextSync] = await Promise.all([getLastSync(), getNextSync()]);
      return {
        lastSync,
        nextSync,
      } as AlmSyncStatus;
    },
    {
      refetchInterval: 10_000,
    },
  );
}

export function useSyncWithGitLabNow() {
  const queryClient = useQueryClient();
  const { data: syncStatus } = useGitLabSyncStatusQuery();
  const { data: gitlabConfigurations } = useGitLabConfigurationsQuery();
  const autoProvisioningEnabled = gitlabConfigurations?.gitlabConfigurations.some(
    (configuration) =>
      configuration.enabled && configuration.provisioningType === ProvisioningType.auto,
  );
  const mutation = useMutation(syncNowGitLabProvisioning, {
    onSuccess: () => {
      queryClient.invalidateQueries(['identity_provider', 'gitlab_sync']);
    },
  });

  return {
    synchronizeNow: mutation.mutate,
    canSyncNow: autoProvisioningEnabled && !syncStatus?.nextSync && !mutation.isLoading,
  };
}
