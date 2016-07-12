/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import { getJSON, post } from '../helpers/request';

export function getQueue (data) {
  const url = window.baseUrl + '/api/ce/queue';
  return $.get(url, data);
}

export function getActivity (data) {
  const url = window.baseUrl + '/api/ce/activity';
  return $.get(url, data);
}

export function getStatus (componentId) {
  const url = '/api/ce/activity_status';
  const data = {};
  if (componentId) {
    Object.assign(data, { componentId });
  }
  return getJSON(url, data);
}

export function getTask (id) {
  const url = '/api/ce/task';
  return getJSON(url, { id }).then(r => r.task);
}

export function cancelTask (id) {
  const url = '/api/ce/cancel';
  return post(url, { id }).then(
      getTask.bind(null, id),
      getTask.bind(null, id)
  );
}

export function cancelAllTasks () {
  const url = '/api/ce/cancel_all';
  return post(url);
}

export function getTasksForComponent (componentId) {
  const url = '/api/ce/component';
  const data = { componentId };
  return getJSON(url, data);
}

export function getTypes () {
  const url = '/api/ce/task_types';
  return getJSON(url).then(r => r.taskTypes);
}
