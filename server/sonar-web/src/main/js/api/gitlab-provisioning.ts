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
import axios from 'axios';
import {
  GitLabConfigurationCreateBody,
  GitLabConfigurationUpdateBody,
  GitlabConfiguration,
  ProvisioningType,
} from '../types/provisioning';
import { Paging } from '../types/types';

const GITLAB_CONFIGURATIONS = '/api/v2/dop-translation/gitlab-configurations';

export function fetchGitLabConfigurations() {
  return axios.get<{ page: Paging; gitlabConfigurations: GitlabConfiguration[] }>(
    GITLAB_CONFIGURATIONS,
  );
}

export function fetchGitLabConfiguration(id: string): Promise<GitlabConfiguration> {
  return axios.get<GitlabConfiguration>(`${GITLAB_CONFIGURATIONS}/${id}`);
}

export function createGitLabConfiguration(
  data: GitLabConfigurationCreateBody,
): Promise<GitlabConfiguration> {
  return axios.post(GITLAB_CONFIGURATIONS, {
    ...data,
    provisioningType: ProvisioningType.jit,
    allowUsersToSignUp: false,
    enabled: true,
  });
}

export function updateGitLabConfiguration(
  id: string,
  data: Partial<GitLabConfigurationUpdateBody>,
) {
  return axios.patch<GitlabConfiguration>(`${GITLAB_CONFIGURATIONS}/${id}`, data);
}

export function deleteGitLabConfiguration(id: string): Promise<void> {
  return axios.delete(`${GITLAB_CONFIGURATIONS}/${id}`);
}

export function syncNowGitLabProvisioning(): Promise<void> {
  return axios.post('/api/v2/dop-translation/gitlab-synchronization-runs');
}
