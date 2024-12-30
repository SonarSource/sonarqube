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

import { QueryClient, queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { addIssueComment, getIssueChangelog, setIssueTransition } from '../api/issues';
import { createQueryHook } from './common';

const issuesQuery = {
  changelog: (issueKey: string) => ['issue', issueKey, 'changelog'] as const,
};

export const useIssueChangelogQuery = createQueryHook((issueKey: string) => {
  return queryOptions({
    queryKey: issuesQuery.changelog(issueKey),
    queryFn: () => getIssueChangelog(issueKey),
  });
});

export function useIssueTransitionMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { issue: string; transition: string }) => setIssueTransition(data),
    onSuccess: ({ issue }) => {
      invalidateIssueChangelog(issue.key, queryClient);
    },
  });
}

export function useIssueCommentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { issue: string; text: string }) => addIssueComment(data),
    onSuccess: ({ issue }) => {
      invalidateIssueChangelog(issue.key, queryClient);
    },
  });
}

function invalidateIssueChangelog(issueKey: string, queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: issuesQuery.changelog(issueKey) });
}
