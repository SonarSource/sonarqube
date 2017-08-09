/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { getJSON, post } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getActivity(data /*: ?Object */) /*: Promise<*> */ {
  return getJSON('/api/ce/activity', data);
}

export function getStatus(componentId /*: ?string */) /*: Promise<*> */ {
  const data = {};
  if (componentId) {
    Object.assign(data, { componentId });
  }
  return getJSON('/api/ce/activity_status', data);
}

export function getTask(
  id /*: string */,
  additionalFields /*: ?Array<string> */
) /*: Promise<*> */ {
  return getJSON('/api/ce/task', { id, additionalFields }).then(r => r.task);
}

export function cancelTask(id /*: string */) /*: Promise<*> */ {
  return post('/api/ce/cancel', { id }).then(() => getTask(id), () => getTask(id));
}

export function cancelAllTasks() /*: Promise<*> */ {
  return post('/api/ce/cancel_all');
}

export function getTasksForComponent(componentKey /*: string */) /*: Promise<*> */ {
  return getJSON('/api/ce/component', { componentKey });
}

export function getTypes() /*: Promise<*> */ {
  return getJSON('/api/ce/task_types').then(r => r.taskTypes);
}

export function getWorkers() /*: Promise<{ canSetWorkerCount: boolean, value: number }> */ {
  return getJSON('/api/ce/worker_count').catch(throwGlobalError);
}

export function setWorkerCount(count /*: number */) /*: Promise<void> */ {
  return post('/api/ce/set_worker_count', { count }).catch(throwGlobalError);
}
