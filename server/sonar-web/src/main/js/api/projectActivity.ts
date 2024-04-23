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
import { getJSON, post, postJSON } from '../helpers/request';
import { throwGlobalError } from '../sonar-aligned/helpers/error';
import { BranchParameters } from '../types/branch-like';
import {
  Analysis,
  ApplicationAnalysisEventCategory,
  ProjectAnalysisEventCategory,
} from '../types/project-activity';
import { Paging } from '../types/types';

export enum ProjectActivityStatuses {
  STATUS_PROCESSED = 'P',
  STATUS_UNPROCESSED = 'U',
  STATUS_LIVE_MEASURE_COMPUTE = 'L',
}

export type ProjectActivityParams = {
  project?: string;
  statuses?: string;
  category?: string;
  from?: string;
  p?: number;
  ps?: number;
} & BranchParameters;

export interface ProjectActivityResponse {
  analyses: Analysis[];
  paging: Paging;
}

export function getProjectActivity(data: ProjectActivityParams): Promise<ProjectActivityResponse> {
  return getJSON('/api/project_analyses/search', data).catch(throwGlobalError);
}

const PROJECT_ACTIVITY_PAGE_SIZE = 500;

export function getAllTimeProjectActivity(
  data: ProjectActivityParams,
  prev?: ProjectActivityResponse,
): Promise<ProjectActivityResponse> {
  return getProjectActivity({ ...data, ps: data.ps ?? PROJECT_ACTIVITY_PAGE_SIZE }).then((r) => {
    const result = prev
      ? {
          analyses: prev.analyses.concat(r.analyses),
          paging: r.paging,
        }
      : r;

    if (result.paging.pageIndex * result.paging.pageSize >= result.paging.total) {
      return result;
    }

    return getAllTimeProjectActivity(
      { ...data, ps: data.ps ?? PROJECT_ACTIVITY_PAGE_SIZE, p: result.paging.pageIndex + 1 },
      result,
    );
  });
}

export interface CreateEventResponse {
  analysis: string;
  key: string;
  name: string;
  category: ProjectAnalysisEventCategory | ApplicationAnalysisEventCategory;
  description?: string;
}

export function createEvent(data: {
  analysis: string;
  name: string;
  category?: string;
  description?: string;
}): Promise<CreateEventResponse> {
  return postJSON('/api/project_analyses/create_event', data).then(
    (r) => r.event,
    throwGlobalError,
  );
}

export function deleteEvent(event: string): Promise<void | Response> {
  return post('/api/project_analyses/delete_event', { event }).catch(throwGlobalError);
}

export function changeEvent(data: {
  event: string;
  name?: string;
  description?: string;
}): Promise<CreateEventResponse> {
  return postJSON('/api/project_analyses/update_event', data).then(
    (r) => r.event,
    throwGlobalError,
  );
}

export function deleteAnalysis(analysis: string): Promise<void | Response> {
  return post('/api/project_analyses/delete', { analysis }).catch(throwGlobalError);
}
