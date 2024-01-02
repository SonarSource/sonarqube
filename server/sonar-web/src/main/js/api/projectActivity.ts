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
import { throwGlobalError } from '../helpers/error';
import { getJSON, post, postJSON, RequestData } from '../helpers/request';
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

export function getProjectActivity(
  data: {
    project: string;
    statuses?: string;
    category?: string;
    from?: string;
    p?: number;
    ps?: number;
  } & BranchParameters,
): Promise<{ analyses: Analysis[]; paging: Paging }> {
  return getJSON('/api/project_analyses/search', data).catch(throwGlobalError);
}

interface CreateEventResponse {
  analysis: string;
  key: string;
  name: string;
  category: ProjectAnalysisEventCategory | ApplicationAnalysisEventCategory;
  description?: string;
}

export function createEvent(
  analysis: string,
  name: string,
  category?: string,
  description?: string,
): Promise<CreateEventResponse> {
  const data: RequestData = { analysis, name };
  if (category) {
    data.category = category;
  }
  if (description) {
    data.description = description;
  }
  return postJSON('/api/project_analyses/create_event', data).then(
    (r) => r.event,
    throwGlobalError,
  );
}

export function deleteEvent(event: string): Promise<void | Response> {
  return post('/api/project_analyses/delete_event', { event }).catch(throwGlobalError);
}

export function changeEvent(
  event: string,
  name?: string,
  description?: string,
): Promise<CreateEventResponse> {
  const data: RequestData = { event };
  if (name) {
    data.name = name;
  }
  if (description) {
    data.description = description;
  }
  return postJSON('/api/project_analyses/update_event', data).then(
    (r) => r.event,
    throwGlobalError,
  );
}

export function deleteAnalysis(analysis: string): Promise<void | Response> {
  return post('/api/project_analyses/delete', { analysis }).catch(throwGlobalError);
}
