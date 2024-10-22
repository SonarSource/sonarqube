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

import axios from 'axios';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { post, postJSON } from '../helpers/request';
import { DevopsRolesMapping, GitHubConfigurationStatus, GithubStatus } from '../types/provisioning';

const GITHUB_PERMISSION_MAPPINGS = '/api/v2/dop-translation/github-permission-mappings';

export function fetchGithubProvisioningStatus(): Promise<GithubStatus> {
  return getJSON('/api/github_provisioning/status').catch(throwGlobalError);
}

export function checkConfigurationValidity(): Promise<GitHubConfigurationStatus> {
  return postJSON('/api/github_provisioning/check').catch(throwGlobalError);
}

export function syncNowGithubProvisioning(): Promise<void> {
  return post('/api/github_provisioning/sync').catch(throwGlobalError);
}

export function fetchGithubRolesMapping() {
  return axios
    .get<{ permissionMappings: DevopsRolesMapping[] }>(GITHUB_PERMISSION_MAPPINGS)
    .then((data) => data.permissionMappings);
}

export function updateGithubRolesMapping(
  role: string,
  data: Partial<Pick<DevopsRolesMapping, 'permissions'>>,
) {
  return axios.patch<DevopsRolesMapping>(
    `${GITHUB_PERMISSION_MAPPINGS}/${encodeURIComponent(role)}`,
    data,
  );
}

export function addGithubRolesMapping(data: Omit<DevopsRolesMapping, 'id'>) {
  return axios.post<DevopsRolesMapping>(GITHUB_PERMISSION_MAPPINGS, data);
}

export function deleteGithubRolesMapping(role: string) {
  return axios.delete(`${GITHUB_PERMISSION_MAPPINGS}/${encodeURIComponent(role)}`);
}
