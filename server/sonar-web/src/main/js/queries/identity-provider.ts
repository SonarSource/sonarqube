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

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { isEqual, keyBy, partition, pick, unionBy } from 'lodash';
import { useContext } from 'react';
import {
  activateGithubProvisioning,
  activateScim,
  addGithubRolesMapping,
  checkConfigurationValidity,
  deactivateGithubProvisioning,
  deactivateScim,
  deleteGithubRolesMapping,
  fetchGithubProvisioningStatus,
  fetchGithubRolesMapping,
  fetchIsScimEnabled,
  syncNowGithubProvisioning,
  updateGithubRolesMapping,
} from '../api/provisioning';
import { getSystemInfo } from '../api/system';
import { AvailableFeaturesContext } from '../app/components/available-features/AvailableFeaturesContext';
import { mapReactQueryResult } from '../helpers/react-query';
import { Feature } from '../types/features';
import { GitHubMapping } from '../types/provisioning';
import { SysInfoCluster } from '../types/types';

const MAPPING_STALE_TIME = 60_000;

export function useIdentityProviderQuery() {
  return useQuery(['identity_provider'], async () => {
    const info = (await getSystemInfo()) as SysInfoCluster;
    return { provider: info.System['External Users and Groups Provisioning'] };
  });
}

export function useScimStatusQuery() {
  const hasScim = useContext(AvailableFeaturesContext).includes(Feature.Scim);

  return useQuery(['identity_provider', 'scim_status'], () => {
    if (!hasScim) {
      return false;
    }
    return fetchIsScimEnabled();
  });
}

export function useToggleScimMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (activate: boolean) => (activate ? activateScim() : deactivateScim()),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['identity_provider'] });
    },
  });
}

export function useToggleGithubProvisioningMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (activate: boolean) =>
      activate ? activateGithubProvisioning() : deactivateGithubProvisioning(),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['identity_provider'] });
    },
  });
}

export const useCheckGitHubConfigQuery = (githubEnabled: boolean) => {
  return useQuery(['identity_provider', 'github_check'], checkConfigurationValidity, {
    enabled: githubEnabled,
  });
};

interface GithubSyncStatusOptions {
  noRefetch?: boolean;
}

export function useGitHubSyncStatusQuery(options: GithubSyncStatusOptions = {}) {
  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning,
  );
  return useQuery(['identity_provider', 'github_sync', 'status'], fetchGithubProvisioningStatus, {
    enabled: hasGithubProvisioning,
    refetchInterval: options.noRefetch ? undefined : 10_000,
  });
}

export function useGithubProvisioningEnabledQuery() {
  const res = useGitHubSyncStatusQuery({ noRefetch: true });

  return mapReactQueryResult(res, (data) => data.enabled);
}

export function useSyncWithGitHubNow() {
  const queryClient = useQueryClient();
  const { data } = useGitHubSyncStatusQuery();
  const mutation = useMutation(syncNowGithubProvisioning, {
    onSuccess: () => {
      queryClient.invalidateQueries(['identity_provider', 'github_sync']);
    },
  });

  return {
    synchronizeNow: mutation.mutate,
    canSyncNow: data?.enabled && !data.nextSync && !mutation.isLoading,
  };
}

// Order is reversed to put custom roles at the end (their index is -1)
const defaultRoleOrder = ['admin', 'maintain', 'write', 'triage', 'read'];

export function useGithubRolesMappingQuery() {
  return useQuery(['identity_provider', 'github_mapping'], fetchGithubRolesMapping, {
    staleTime: MAPPING_STALE_TIME,
    select: (data) =>
      [...data].sort((a, b) => {
        if (defaultRoleOrder.includes(a.id) || defaultRoleOrder.includes(b.id)) {
          return defaultRoleOrder.indexOf(b.id) - defaultRoleOrder.indexOf(a.id);
        }
        return a.githubRole.localeCompare(b.githubRole);
      }),
  });
}

export function useGithubRolesMappingMutation() {
  const client = useQueryClient();
  const queryKey = ['identity_provider', 'github_mapping'];
  return useMutation({
    mutationFn: async (mapping: GitHubMapping[]) => {
      const state = keyBy(client.getQueryData<GitHubMapping[]>(queryKey), (m) => m.id);

      const [maybeChangedRoles, newRoles] = partition(mapping, (m) => state[m.id]);
      const changedRoles = maybeChangedRoles.filter((item) => !isEqual(item, state[item.id]));
      const deletedRoles = Object.values(state).filter(
        (m) => !m.isBaseRole && !mapping.some((cm) => m.id === cm.id),
      );

      return {
        addedOrChanged: await Promise.all([
          ...changedRoles.map((data) =>
            updateGithubRolesMapping(data.id, pick(data, 'permissions')),
          ),
          ...newRoles.map((m) => addGithubRolesMapping(m)),
        ]),
        deleted: await Promise.all([
          deletedRoles.map((dm) => deleteGithubRolesMapping(dm.id)),
        ]).then(() => deletedRoles.map((dm) => dm.id)),
      };
    },
    onSuccess: ({ addedOrChanged, deleted }) => {
      const state = client.getQueryData<GitHubMapping[]>(queryKey);
      if (state) {
        const newData = unionBy(
          addedOrChanged,
          state.filter((s) => !deleted.find((id) => id === s.id)),
          (el) => el.id,
        );
        client.setQueryData(queryKey, newData);
      }
    },
  });
}
