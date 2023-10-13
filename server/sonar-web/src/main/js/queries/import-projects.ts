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
import { useIsMutating, useMutation } from '@tanstack/react-query';
import {
  importAzureRepository,
  importBitbucketCloudRepository,
  importBitbucketServerProject,
  importGithubRepository,
  importGitlabProject,
} from '../api/alm-integrations';
import { createProject } from '../api/project-management';
import { ImportProjectParam } from '../apps/create/project/CreateProjectPage';
import { CreateProjectModes } from '../apps/create/project/types';

export type MutationArg<AlmImport extends ImportProjectParam = ImportProjectParam> =
  AlmImport extends {
    creationMode: infer A;
    almSetting: string;
    projects: (infer R)[];
  }
    ? { creationMode: A; almSetting: string } & R
    : never;

export function useImportProjectMutation() {
  return useMutation({
    mutationFn: (
      data: {
        newCodeDefinitionType?: string;
        newCodeDefinitionValue?: string;
      } & MutationArg,
    ) => {
      if (data.creationMode === CreateProjectModes.GitHub) {
        return importGithubRepository(data);
      } else if (data.creationMode === CreateProjectModes.AzureDevOps) {
        return importAzureRepository(data);
      } else if (data.creationMode === CreateProjectModes.BitbucketCloud) {
        return importBitbucketCloudRepository(data);
      } else if (data.creationMode === CreateProjectModes.BitbucketServer) {
        return importBitbucketServerProject(data);
      } else if (data.creationMode === CreateProjectModes.GitLab) {
        return importGitlabProject(data);
      }

      return createProject(data);
    },
    mutationKey: ['import'],
  });
}

export function useImportProjectProgress() {
  return useIsMutating({ mutationKey: ['import'] });
}
