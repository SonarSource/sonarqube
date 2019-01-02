/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON, post, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getAnalysisStatus(data: {
  component: string;
  branch?: string;
  pullRequest?: string;
}): Promise<{
  component: {
    branch?: string;
    key: string;
    name: string;
    organization?: string;
    pullRequest?: string;
    warnings: string[];
  };
}> {
  return getJSON('/api/ce/analysis_status', data).catch(throwGlobalError);
}

export function getActivity(data: RequestData): Promise<{ tasks: T.Task[] }> {
  return getJSON('/api/ce/activity', data);
}

export function getStatus(
  componentId?: string
): Promise<{ failing: number; inProgress: number; pending: number }> {
  const data = {};
  if (componentId) {
    Object.assign(data, { componentId });
  }
  return getJSON('/api/ce/activity_status', data);
}

export function getTask(id: string, additionalFields?: string[]): Promise<T.Task> {
  return getJSON('/api/ce/task', { id, additionalFields }).then(r => r.task);
}

export function cancelTask(id: string): Promise<any> {
  return post('/api/ce/cancel', { id }).then(() => getTask(id), () => getTask(id));
}

export function cancelAllTasks(): Promise<any> {
  return post('/api/ce/cancel_all');
}

export function getTasksForComponent(
  component: string
): Promise<{ queue: T.Task[]; current: T.Task }> {
  return getJSON('/api/ce/component', { component }).catch(throwGlobalError);
}

export function getTypes(): Promise<string[]> {
  return getJSON('/api/ce/task_types').then(r => r.taskTypes);
}

export function getWorkers(): Promise<{ canSetWorkerCount: boolean; value: number }> {
  return getJSON('/api/ce/worker_count').catch(throwGlobalError);
}

export function setWorkerCount(count: number): Promise<void | Response> {
  return post('/api/ce/set_worker_count', { count }).catch(throwGlobalError);
}
