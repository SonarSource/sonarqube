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
import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { searchProjects } from '../api/components';
import { createProject, deleteProject } from '../api/project-management';
import { createQueryHook } from './common';
import { invalidateMeasuresByComponentKey } from './measures';

export const useProjectQuery = createQueryHook((key: string) => {
  return queryOptions({
    queryKey: ['project', key],
    queryFn: ({ queryKey: [, key] }) => searchProjects({ filter: `query=${key}` }),
  });
});

export function useDeleteProjectMutation(organization: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => deleteProject(organization, key),
    onSuccess: (_, key) => {
      invalidateMeasuresByComponentKey(key, queryClient);
    },
  });
}

export function useCreateProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Parameters<typeof createProject>[0]) => createProject(data),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ['project', 'list'] });
    },
  });
}
