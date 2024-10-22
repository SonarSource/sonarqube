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
import { addGlobalSuccessMessage } from '~design-system';
import {
  createGitHubConfiguration,
  deleteGitHubConfiguration,
  fetchGitHubConfiguration,
  getProjectBindings,
  searchGitHubConfigurations,
  updateGitHubConfiguration,
} from '../api/dop-translation';
import { translate } from '../helpers/l10n';
import { ProvisioningType } from '../types/provisioning';
import { useSyncWithGitHubNow } from './identity-provider/github';

/*
 * Project bindings
 */
export function useProjectBindingsQuery(
  data: {
    dopSettingId?: string;
    pageIndex?: number;
    pageSize?: number;
    repository?: string;
  },
  enabled = true,
) {
  return useQuery({
    enabled,
    queryKey: ['dop-translation', 'project-bindings', data],
    queryFn: () => getProjectBindings(data),
  });
}

/*
 * GitHub configurations
 */
export function useSearchGitHubConfigurationsQuery() {
  return useQuery({
    queryKey: ['dop-translation', 'github-configs', 'search'],
    queryFn: searchGitHubConfigurations,
  });
}

export function useFetchGitHubConfigurationQuery(id: string) {
  return useQuery({
    queryKey: ['dop-translation', 'github-configs', 'fetch'],
    queryFn: () => fetchGitHubConfiguration(id),
  });
}

export function useCreateGitHubConfigurationMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (gitHubConfiguration: Parameters<typeof createGitHubConfiguration>[0]) =>
      createGitHubConfiguration(gitHubConfiguration),
    onSuccess(gitHubConfiguration) {
      client.setQueryData(['dop-translation', 'github-configs', 'search'], {
        githubConfigurations: [gitHubConfiguration],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 1,
        },
      });
      client.setQueryData(['dop-translation', 'github-configs', 'fetch'], gitHubConfiguration);
    },
  });
}

export function useUpdateGitHubConfigurationMutation() {
  const client = useQueryClient();
  const { canSyncNow, synchronizeNow } = useSyncWithGitHubNow();
  return useMutation({
    mutationFn: ({
      gitHubConfiguration,
      id,
    }: {
      gitHubConfiguration: Parameters<typeof updateGitHubConfiguration>[1];
      id: Parameters<typeof updateGitHubConfiguration>[0];
    }) => updateGitHubConfiguration(id, gitHubConfiguration),
    onSuccess(gitHubConfiguration) {
      client.setQueryData(['dop-translation', 'github-configs', 'search'], {
        githubConfigurations: [gitHubConfiguration],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 1,
        },
      });
      client.setQueryData(['dop-translation', 'github-configs', 'fetch'], gitHubConfiguration);
      client.invalidateQueries({ queryKey: ['identity_provider'] });
      if (canSyncNow && gitHubConfiguration.provisioningType === ProvisioningType.auto) {
        synchronizeNow();
      }
      addGlobalSuccessMessage(translate('settings.authentication.form.settings.save_success'));
    },
  });
}

export function useDeleteGitHubConfigurationMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: Parameters<typeof deleteGitHubConfiguration>[0]) =>
      deleteGitHubConfiguration(id),
    onSuccess() {
      client.setQueryData(['dop-translation', 'github-configs', 'search'], {
        githubConfigurations: [],
        page: {
          pageIndex: 1,
          pageSize: 1,
          total: 1,
        },
      });
      client.setQueryData(['dop-translation', 'github-configs', 'fetch'], undefined);
    },
  });
}
