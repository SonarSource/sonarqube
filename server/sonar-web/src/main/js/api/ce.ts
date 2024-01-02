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
import { getJSON, post } from '../helpers/request';
import { IndexationStatus } from '../types/indexation';
import { ActivityRequestParameters, Task, TaskWarning } from '../types/tasks';
import { Paging } from '../types/types';

export function getAnalysisStatus(data: {
  component: string;
  branch?: string;
  pullRequest?: string;
}): Promise<{
  component: {
    branch?: string;
    key: string;
    name: string;
    pullRequest?: string;
    warnings: TaskWarning[];
  };
}> {
  return getJSON('/api/ce/analysis_status', data).catch(throwGlobalError);
}

export function getActivity(
  data: ActivityRequestParameters
): Promise<{ tasks: Task[]; paging: Paging }> {
  return getJSON('/api/ce/activity', data);
}

export function getStatus(
  component?: string
): Promise<{ failing: number; inProgress: number; pending: number; pendingTime?: number }> {
  return getJSON('/api/ce/activity_status', { component });
}

export function getTask(id: string, additionalFields?: string[]): Promise<Task> {
  return getJSON('/api/ce/task', { id, additionalFields }).then((r) => r.task);
}

export function cancelTask(id: string): Promise<any> {
  return post('/api/ce/cancel', { id }).then(
    () => getTask(id),
    () => getTask(id)
  );
}

export function cancelAllTasks(): Promise<any> {
  return post('/api/ce/cancel_all');
}

export function getTasksForComponent(component: string): Promise<{ queue: Task[]; current: Task }> {
  return getJSON('/api/ce/component', { component }).catch(throwGlobalError);
}

export function getTypes(): Promise<string[]> {
  return getJSON('/api/ce/task_types').then((r) => r.taskTypes);
}

export function getWorkers(): Promise<{ canSetWorkerCount: boolean; value: number }> {
  return getJSON('/api/ce/worker_count').catch(throwGlobalError);
}

export function setWorkerCount(count: number): Promise<void | Response> {
  return post('/api/ce/set_worker_count', { count }).catch(throwGlobalError);
}

export function getIndexationStatus(): Promise<IndexationStatus> {
  return getJSON('/api/ce/indexation_status').catch(throwGlobalError);
}

export function dismissAnalysisWarning(component: string, warning: string) {
  return post('/api/ce/dismiss_analysis_warning', { component, warning }).catch(throwGlobalError);
}
