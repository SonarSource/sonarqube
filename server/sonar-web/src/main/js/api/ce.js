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

export function getQueue (data) {
  const url = baseUrl + '/api/ce/queue';
  return $.get(url, data);
}

export function getActivity (data) {
  const url = baseUrl + '/api/ce/activity';
  return $.get(url, data);
}

export function getTask (id) {
  const url = baseUrl + '/api/ce/task';
  return $.get(url, { id });
}

export function cancelTask (id) {
  const url = baseUrl + '/api/ce/cancel';
  return $.post(url, { id }).then(getTask.bind(null, id));
}

export function cancelAllTasks () {
  const url = baseUrl + '/api/ce/cancel_all';
  return $.post(url);
}

export function getTasksForComponent(componentId) {
  const url = baseUrl + '/api/ce/component';
  const data = { componentId };
  return new Promise((resolve) => $.get(url, data).done(resolve));
}
