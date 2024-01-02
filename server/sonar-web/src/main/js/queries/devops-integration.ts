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
import { UseQueryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import {
  deleteProjectAlmBinding,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectBitbucketCloudBinding,
  setProjectGithubBinding,
  setProjectGitlabBinding,
} from '../api/alm-settings';
import { HttpStatus } from '../helpers/request';
import { AlmKeys, ProjectAlmBindingParams, ProjectAlmBindingResponse } from '../types/alm-settings';

function useProjectKeyFromLocation() {
  const location = useLocation();
  const search = new URLSearchParams(location.search);
  const id = search.get('id');
  return id as string;
}

export function useProjectBindingQuery<T = ProjectAlmBindingResponse>(
  project?: string,
  options?: UseQueryOptions<
    ProjectAlmBindingResponse,
    unknown,
    T,
    ['devops_integration', string, 'binding']
  >,
) {
  const keyFromUrl = useProjectKeyFromLocation();

  const projectKey = project ?? keyFromUrl;

  return useQuery(
    ['devops_integration', projectKey, 'binding'],
    ({ queryKey: [_, key] }) =>
      getProjectAlmBinding(key).catch((e: Response) => {
        if (e.status === HttpStatus.NotFound) {
          return null;
        }
        throw e;
      }),
    {
      staleTime: 60_000,
      retry: false,
      ...options,
    },
  );
}

export function useIsGitHubProjectQuery(project?: string) {
  return useProjectBindingQuery(project, { select: (data) => data?.alm === AlmKeys.GitHub });
}

export function useDeleteProjectAlmBindingMutation(project?: string) {
  const keyFromUrl = useProjectKeyFromLocation();
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => deleteProjectAlmBinding(project ?? keyFromUrl),
    onSuccess: () => {
      client.invalidateQueries(['devops_integration', project ?? keyFromUrl, 'binding']);
    },
  });
}

const getSetProjectBindingFn = (data: SetBindingParams) => {
  const { alm, almSetting, project, monorepo, slug, repository, summaryCommentEnabled } = data;
  switch (alm) {
    case AlmKeys.Azure: {
      return setProjectAzureBinding({
        almSetting,
        project,
        projectName: slug,
        repositoryName: repository,
        monorepo,
      });
    }
    case AlmKeys.BitbucketServer: {
      return setProjectBitbucketBinding({
        almSetting,
        project,
        repository,
        slug,
        monorepo,
      });
    }
    case AlmKeys.BitbucketCloud: {
      return setProjectBitbucketCloudBinding({
        almSetting,
        project,
        repository,
        monorepo,
      });
    }
    case AlmKeys.GitHub: {
      return setProjectGithubBinding({
        almSetting,
        project,
        repository,
        summaryCommentEnabled,
        monorepo,
      });
    }

    case AlmKeys.GitLab: {
      return setProjectGitlabBinding({
        almSetting,
        project,
        repository,
        monorepo,
      });
    }

    default:
      return Promise.reject();
  }
};

type SetBindingParams = ProjectAlmBindingParams & {
  repository: string;
} & (
    | { alm: AlmKeys.Azure | AlmKeys.BitbucketServer; slug: string; summaryCommentEnabled?: never }
    | { alm: AlmKeys.GitHub; summaryCommentEnabled: boolean; slug?: never }
    | {
        alm: Exclude<AlmKeys, AlmKeys.Azure | AlmKeys.GitHub | AlmKeys.BitbucketServer>;
        slug?: never;
        summaryCommentEnabled?: never;
      }
  );

export function useSetProjectBindingMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (data: SetBindingParams) => getSetProjectBindingFn(data),
    onSuccess: (_, variables) => {
      client.invalidateQueries(['devops_integration', variables.project, 'binding']);
    },
  });
}
