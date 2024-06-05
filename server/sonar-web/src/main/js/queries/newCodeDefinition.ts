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
// React-query component for new code definition

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getNewCodeDefinition,
  resetNewCodeDefinition,
  setNewCodeDefinition,
} from '../api/newCodeDefinition';
import { NewCodeDefinitionType } from '../types/new-code-definition';

function getNewCodeDefinitionQueryKey(projectKey?: string, branchName?: string) {
  return ['new-code-definition', { projectKey, branchName }];
}

export function useNewCodeDefinitionQuery(params?: {
  branchName?: string;
  enabled?: boolean;
  projectKey?: string;
}) {
  return useQuery({
    queryKey: getNewCodeDefinitionQueryKey(params?.projectKey, params?.branchName),
    queryFn: () =>
      getNewCodeDefinition({ branch: params?.branchName, project: params?.projectKey }),
    enabled: params?.enabled ?? true,
    refetchOnWindowFocus: false,
  });
}

export function useNewCodeDefinitionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newCodeDefinition: {
      branch?: string;
      project?: string;
      type?: NewCodeDefinitionType;
      value?: string;
    }) => {
      const { branch, project, type, value } = newCodeDefinition;

      if (type === undefined) {
        return resetNewCodeDefinition({
          branch,
          project,
        });
      }

      return setNewCodeDefinition({ branch, project, type, value });
    },
    onSuccess(_, { branch, project }) {
      queryClient.invalidateQueries({
        queryKey: getNewCodeDefinitionQueryKey(project, branch),
      });
    },
  });
}
