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
import { isEqual, keyBy, partition, pick, unionBy } from 'lodash';
import { useContext } from 'react';
import { addGlobalSuccessMessage } from '~design-system';
import {
  addGithubRolesMapping,
  checkConfigurationValidity,
  deleteGithubRolesMapping,
  fetchGithubProvisioningStatus,
  fetchGithubRolesMapping,
  syncNowGithubProvisioning,
  updateGithubRolesMapping,
} from '../../api/github-provisioning';
import { AvailableFeaturesContext } from '../../app/components/available-features/AvailableFeaturesContext';
import { translate } from '../../helpers/l10n';
import { mapReactQueryResult } from '../../helpers/react-query';
import { Feature } from '../../types/features';
import { DevopsRolesMapping } from '../../types/provisioning';
import { StaleTime, createQueryHook } from '../common';

export const useCheckGitHubConfigQuery = (githubEnabled: boolean) => {
  return useQuery({
    queryKey: ['identity_provider', 'github_check'],
    queryFn: checkConfigurationValidity,
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
  return useQuery({
    queryKey: ['identity_provider', 'github_sync', 'status'],
    queryFn: fetchGithubProvisioningStatus,
    enabled: hasGithubProvisioning,
    refetchInterval: options.noRefetch ? undefined : 10_000,
  });
}

export function useGithubProvisioningEnabledQuery() {
  /*
  const res = useGitHubSyncStatusQuery({ noRefetch: true });

  return mapReactQueryResult(res, (data) => {
    return data.enabled;
  });
   */
  return { data: false };
}

export function useSyncWithGitHubNow() {
  const queryClient = useQueryClient();
  const { data } = useGitHubSyncStatusQuery();
  const mutation = useMutation({
    mutationFn: syncNowGithubProvisioning,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['identity_provider', 'github_sync'] });
    },
  });

  return {
    synchronizeNow: mutation.mutate,
    canSyncNow: data?.enabled && !data.nextSync && !mutation.isPending,
  };
}

// Order is reversed to put custom roles at the end (their index is -1)
const defaultRoleOrder = ['admin', 'maintain', 'write', 'triage', 'read'];

function sortGithubRoles(data: DevopsRolesMapping[]) {
  return [...data].sort((a, b) => {
    if (defaultRoleOrder.includes(a.id) || defaultRoleOrder.includes(b.id)) {
      return defaultRoleOrder.indexOf(b.id) - defaultRoleOrder.indexOf(a.id);
    }
    return a.role.localeCompare(b.role);
  });
}

export const useGithubRolesMappingQuery = createQueryHook(() => {
  return queryOptions({
    queryKey: ['identity_provider', 'github_mapping'],
    queryFn: fetchGithubRolesMapping,
    staleTime: StaleTime.LONG,
    select: sortGithubRoles, // uses a stable function reference
  });
});

export function useGithubRolesMappingMutation() {
  const client = useQueryClient();
  const queryKey = ['identity_provider', 'github_mapping'];
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
        translate('settings.authentication.github.configuration.roles_mapping.save_success'),
      );
    },
  });
}
