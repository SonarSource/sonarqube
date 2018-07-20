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
/* @flow */
import { STATUSES, ALL_TYPES, CURRENTS } from './constants';
import { toShortNotSoISOString } from '../../helpers/dates';
/*:: import type { Task } from './types'; */

export function updateTask(tasks /*: Task[] */, newTask /*: Task */) {
  return tasks.map(task => (task.id === newTask.id ? newTask : task));
}

export function mapFiltersToParameters(filters /*: Object */ = {}) {
  const parameters = {};

  if (filters.status === STATUSES.ALL) {
    parameters.status = [
      STATUSES.PENDING,
      STATUSES.IN_PROGRESS,
      STATUSES.SUCCESS,
      STATUSES.FAILED,
      STATUSES.CANCELED
    ].join();
  } else if (filters.status === STATUSES.ALL_EXCEPT_PENDING) {
    parameters.status = [
      STATUSES.IN_PROGRESS,
      STATUSES.SUCCESS,
      STATUSES.FAILED,
      STATUSES.CANCELED
    ].join();
  } else {
    parameters.status = filters.status;
  }

  if (filters.taskType !== ALL_TYPES) {
    parameters.type = filters.taskType;
  }

  if (filters.currents !== CURRENTS.ALL) {
    parameters.onlyCurrents = true;
  }

  if (filters.minSubmittedAt) {
    parameters.minSubmittedAt = toShortNotSoISOString(filters.minSubmittedAt);
  }

  if (filters.maxExecutedAt) {
    parameters.maxExecutedAt = toShortNotSoISOString(filters.maxExecutedAt);
  }

  if (filters.query) {
    parameters.componentQuery = filters.query;
  }

  if (filters.lastPage !== 1) {
    parameters.p = filters.lastPage;
  }

  return parameters;
}

const ONE_SECOND = 1000;
const ONE_MINUTE = 60 * ONE_SECOND;
const ONE_HOUR = 60 * ONE_MINUTE;

function format(int, suffix) {
  return `${int}${suffix}`;
}

export function formatDuration(value /*: ?number */) {
  if (!value) {
    return '';
  }
  if (value < ONE_SECOND) {
    return format(value, 'ms');
  } else if (value < ONE_SECOND * 10) {
    const seconds = Math.floor(value / ONE_SECOND);
    const ms = value - seconds * ONE_SECOND;
    return seconds + '.' + format(ms, 's');
  } else if (value < ONE_MINUTE) {
    const seconds = Math.floor(value / ONE_SECOND);
    return format(seconds, 's');
  } else if (value < ONE_MINUTE * 10) {
    const minutes = Math.floor(value / ONE_MINUTE);
    const seconds = Math.floor((value - minutes * ONE_MINUTE) / ONE_SECOND);
    return format(minutes, 'min') + ' ' + format(seconds, 's');
  }
  const hours = Math.floor(value / ONE_HOUR);
  const minutes = Math.floor((value - hours * ONE_HOUR) / ONE_MINUTE);
  return format(hours, 'h') + ' ' + format(minutes, 'min');
}
