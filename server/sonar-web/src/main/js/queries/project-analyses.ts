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
import {
  CreateEventResponse,
  ProjectActivityStatuses,
  changeEvent,
  createEvent,
  deleteAnalysis,
  deleteEvent,
  getAllTimeProjectActivity,
} from '../api/projectActivity';
import {
  useComponent,
  useTopLevelComponentKey,
} from '../app/components/componentContext/withComponentContext';
import { getBranchLikeQuery } from '../helpers/branch-like';
import { parseDate } from '../helpers/dates';
import { serializeStringArray } from '../helpers/query';
import { BranchParameters } from '../types/branch-like';
import { ParsedAnalysis } from '../types/project-activity';
import { useBranchesQuery } from './branch';

const ACTIVITY_PAGE_SIZE = 500;

function useProjectActivityQueryKey() {
  const { component } = useComponent();
  const componentKey = useTopLevelComponentKey();
  const { data: { branchLike } = {} } = useBranchesQuery(component);
  const branchParams = getBranchLikeQuery(branchLike);

  return ['activity', 'list', componentKey, branchParams] as [
    string,
    string,
    string | undefined,
    BranchParameters,
  ];
}

export function useAllProjectAnalysesQuery(enabled = true) {
  const queryKey = useProjectActivityQueryKey();
  return useQuery({
    queryKey,
    queryFn: ({ queryKey: [_0, _1, project, branchParams] }) =>
      getAllTimeProjectActivity({
        ...branchParams,
        project,
        statuses: serializeStringArray([
          ProjectActivityStatuses.STATUS_PROCESSED,
          ProjectActivityStatuses.STATUS_LIVE_MEASURE_COMPUTE,
        ]),
        p: 1,
        ps: ACTIVITY_PAGE_SIZE,
      }).then(({ analyses }) =>
        analyses.map((analysis) => ({
          ...analysis,
          date: parseDate(analysis.date),
        })),
      ),
    enabled,
  });
}

export function useDeleteAnalysisMutation(successCb?: () => void) {
  const queryClient = useQueryClient();
  const queryKey = useProjectActivityQueryKey();

  return useMutation({
    mutationFn: (analysis: string) => deleteAnalysis(analysis),
    onSuccess: (_, analysis) => {
      queryClient.setQueryData(queryKey, (oldData: ParsedAnalysis[]) =>
        oldData.filter((a) => a.key !== analysis),
      );
      queryClient.invalidateQueries({ queryKey: ['measures', 'history', queryKey[2]] });
      successCb?.();
    },
  });
}

export function useCreateEventMutation(successCb?: () => void) {
  const queryClient = useQueryClient();
  const queryKey = useProjectActivityQueryKey();

  return useMutation({
    mutationFn: (data: Parameters<typeof createEvent>[0]) => createEvent(data),
    onSuccess: (event) => {
      queryClient.setQueryData(queryKey, (oldData: ParsedAnalysis[]) => {
        return oldData.map((analysis) => {
          if (analysis.key !== event.analysis) {
            return analysis;
          }
          return { ...analysis, events: [...analysis.events, event] };
        });
      });
      successCb?.();
    },
  });
}

export function useChangeEventMutation(successCb?: () => void) {
  const queryClient = useQueryClient();
  const queryKey = useProjectActivityQueryKey();

  return useMutation({
    mutationFn: (data: Parameters<typeof changeEvent>[0]) => changeEvent(data),
    onSuccess: (event) => {
      queryClient.setQueryData(queryKey, updateQueryDataOnChangeEvent(event));
      successCb?.();
    },
  });
}

const updateQueryDataOnChangeEvent =
  (event: CreateEventResponse) => (oldData: ParsedAnalysis[]) => {
    return oldData.map((a) => {
      if (a.key !== event.analysis) {
        return a;
      }
      return {
        ...a,
        events: a.events.map((e) => (e.key === event.key ? event : e)),
      };
    });
  };

export function useDeleteEventMutation(successCb?: () => void) {
  const queryClient = useQueryClient();
  const queryKey = useProjectActivityQueryKey();

  return useMutation({
    mutationFn: ({ event }: { analysis: string; event: string }) => deleteEvent(event),
    onSuccess: (_, variables) => {
      queryClient.setQueryData(queryKey, updateQueryDataOnDeleteEvent(variables));
      successCb?.();
    },
  });
}

const updateQueryDataOnDeleteEvent =
  ({ analysis, event }: { analysis: string; event: string }) =>
  (oldData: ParsedAnalysis[]) => {
    return oldData.map((a) => {
      if (a.key !== analysis) {
        return a;
      }
      return {
        ...a,
        events: a.events.filter((ev) => ev.key !== event),
      };
    });
  };
