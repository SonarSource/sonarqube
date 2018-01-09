/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

export interface PendingTask {
  componentKey: string;
  componentName: string;
  componentQualifier: string;
  id: string;
  logs: boolean;
  organization: string;
  status: string;
  submittedAt: Date;
  submitterLogin?: string;
  type: string;
}

export interface Task extends PendingTask {
  analysisId?: string;
  errorMessage?: string;
  errorType?: string;
  executionTimeMs: number;
  executedAt: Date;
  hasErrorStacktrace: boolean;
  hasScannerContext: boolean;
  startedAt: Date;
}

export function getActivity(data: RequestData): Promise<any> {
  return getJSON('/api/ce/activity', data);
}

export function getStatus(componentId?: string): Promise<any> {
  const data = {};
  if (componentId) {
    Object.assign(data, { componentId });
  }
  return getJSON('/api/ce/activity_status', data);
}

export function getTask(id: string, additionalFields?: string[]): Promise<any> {
  return getJSON('/api/ce/task', { id, additionalFields }).then(r => r.task);
}

export function cancelTask(id: string): Promise<any> {
  return post('/api/ce/cancel', { id }).then(() => getTask(id), () => getTask(id));
}

export function cancelAllTasks(): Promise<any> {
  return post('/api/ce/cancel_all');
}

export function getTasksForComponent(
  componentKey: string
): Promise<{ queue: PendingTask[]; current: Task }> {
  return getJSON('/api/ce/component', { componentKey }).catch(throwGlobalError);
}

export function getTypes(): Promise<any> {
  return getJSON('/api/ce/task_types').then(r => r.taskTypes);
}

export function getWorkers(): Promise<{ canSetWorkerCount: boolean; value: number }> {
  return getJSON('/api/ce/worker_count').catch(throwGlobalError);
}

export function setWorkerCount(count: number): Promise<void | Response> {
  return post('/api/ce/set_worker_count', { count }).catch(throwGlobalError);
}
