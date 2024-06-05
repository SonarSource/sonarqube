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
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { get, parseError, post, postJSON } from '../helpers/request';
import {
  AzureProject,
  AzureRepository,
  BitbucketCloudRepository,
  BitbucketProject,
  BitbucketRepository,
  GithubOrganization,
  GithubRepository,
  GitlabProject,
} from '../types/alm-integration';
import { Paging } from '../types/types';
import { ProjectBase } from './components';

export function setAlmPersonalAccessToken(
  almSetting: string,
  pat: string,
  username?: string,
): Promise<void> {
  return post('/api/alm_integrations/set_pat', { almSetting, pat, username }).catch(
    throwGlobalError,
  );
}

export function checkPersonalAccessTokenIsValid(
  almSetting: string,
): Promise<{ error?: string; status: boolean }> {
  return get('/api/alm_integrations/check_pat', { almSetting })
    .then(() => ({ status: true }))
    .catch(async (response: Response) => {
      if (response.status === 400) {
        const error = await parseError(response);
        return { status: false, error };
      }
      return throwGlobalError(response);
    });
}

export function getAzureProjects(almSetting: string): Promise<{ projects: AzureProject[] }> {
  return getJSON('/api/alm_integrations/list_azure_projects', { almSetting }).catch(
    throwGlobalError,
  );
}

export function getAzureRepositories(
  almSetting: string,
  projectName: string,
): Promise<{ repositories: AzureRepository[] }> {
  return getJSON('/api/alm_integrations/search_azure_repos', { almSetting, projectName }).catch(
    throwGlobalError,
  );
}

export function searchAzureRepositories(
  almSetting: string,
  searchQuery: string,
): Promise<{ repositories: AzureRepository[] }> {
  return getJSON('/api/alm_integrations/search_azure_repos', { almSetting, searchQuery }).catch(
    throwGlobalError,
  );
}

export function setupAzureProjectCreation(data: {
  almSetting: string;
  projectName: string;
  repositoryName: string;
}) {
  return (newCodeDefinitionType?: string, newCodeDefinitionValue?: string) =>
    importAzureRepository({ ...data, newCodeDefinitionType, newCodeDefinitionValue });
}

export function importAzureRepository(data: {
  almSetting: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
  projectName: string;
  repositoryName: string;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_azure_project', data).catch(throwGlobalError);
}

export function getBitbucketServerProjects(
  almSetting: string,
): Promise<{ projects: BitbucketProject[] }> {
  return getJSON('/api/alm_integrations/list_bitbucketserver_projects', { almSetting });
}

export function getBitbucketServerRepositories(
  almSetting: string,
  projectName: string,
): Promise<{
  isLastPage: boolean;
  repositories: BitbucketRepository[];
}> {
  return getJSON('/api/alm_integrations/search_bitbucketserver_repos', {
    almSetting,
    projectName,
  });
}

export function setupBitbucketServerProjectCreation(data: {
  almSetting: string;
  projectKey: string;
  repositorySlug: string;
}) {
  return (newCodeDefinitionType?: string, newCodeDefinitionValue?: string) =>
    importBitbucketServerProject({ ...data, newCodeDefinitionType, newCodeDefinitionValue });
}

export function importBitbucketServerProject(data: {
  almSetting: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
  projectKey: string;
  repositorySlug: string;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_bitbucketserver_project', data).catch(
    throwGlobalError,
  );
}

export function searchForBitbucketServerRepositories(
  almSetting: string,
  repositoryName: string,
): Promise<{
  isLastPage: boolean;
  repositories: BitbucketRepository[];
}> {
  return getJSON('/api/alm_integrations/search_bitbucketserver_repos', {
    almSetting,
    repositoryName,
  });
}

export function searchForBitbucketCloudRepositories(
  almSetting: string,
  repositoryName: string,
  pageSize: number,
  page?: number,
): Promise<{
  isLastPage: boolean;
  repositories: BitbucketCloudRepository[];
}> {
  return getJSON('/api/alm_integrations/search_bitbucketcloud_repos', {
    almSetting,
    repositoryName,
    p: page,
    ps: pageSize,
  });
}

export function getGithubClientId(almSetting: string): Promise<{ clientId?: string }> {
  return getJSON('/api/alm_integrations/get_github_client_id', { almSetting });
}

export function setupBitbucketCloudProjectCreation(data: {
  almSetting: string;
  repositorySlug: string;
}) {
  return (newCodeDefinitionType?: string, newCodeDefinitionValue?: string) =>
    importBitbucketCloudRepository({ ...data, newCodeDefinitionType, newCodeDefinitionValue });
}

export function importBitbucketCloudRepository(data: {
  almSetting: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
  repositorySlug: string;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_bitbucketcloud_repo', data).catch(throwGlobalError);
}

export function setupGithubProjectCreation(data: {
  almSetting: string;
  organization: string;
  repositoryKey: string;
}) {
  return (newCodeDefinitionType?: string, newCodeDefinitionValue?: string) =>
    importGithubRepository({ ...data, newCodeDefinitionType, newCodeDefinitionValue });
}

export function importGithubRepository(data: {
  almSetting: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
  repositoryKey: string;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_github_project', data).catch(throwGlobalError);
}

export function getGithubOrganizations(
  almSetting: string,
  token: string,
): Promise<{ organizations: GithubOrganization[] }> {
  return getJSON('/api/alm_integrations/list_github_organizations', {
    almSetting,
    token,
  }).catch((response?: Response) => {
    if (response && response.status !== 400) {
      throwGlobalError(response);
    }
  });
}

export function getGithubRepositories(data: {
  almSetting: string;
  organization: string;
  page?: number;
  pageSize: number;
  query?: string;
}): Promise<{ paging: Paging; repositories: GithubRepository[] }> {
  const { almSetting, organization, pageSize, page = 1, query } = data;
  return getJSON('/api/alm_integrations/list_github_repositories', {
    almSetting,
    organization,
    p: page,
    ps: pageSize,
    q: query || undefined,
  }).catch(throwGlobalError);
}

export function getGitlabProjects(data: {
  almSetting: string;
  page?: number;
  pageSize?: number;
  query?: string;
}): Promise<{ projects: GitlabProject[]; projectsPaging: Paging }> {
  const { almSetting, pageSize, page, query } = data;
  return getJSON('/api/alm_integrations/search_gitlab_repos', {
    almSetting,
    projectName: query || undefined,
    p: page,
    ps: pageSize,
  })
    .then(({ repositories, paging }) => ({ projects: repositories, projectsPaging: paging }))
    .catch(throwGlobalError);
}

export function setupGitlabProjectCreation(data: { almSetting: string; gitlabProjectId: string }) {
  return (newCodeDefinitionType?: string, newCodeDefinitionValue?: string) =>
    importGitlabProject({ ...data, newCodeDefinitionType, newCodeDefinitionValue });
}

export function importGitlabProject(data: {
  almSetting: string;
  gitlabProjectId: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/alm_integrations/import_gitlab_project', data).catch(throwGlobalError);
}
