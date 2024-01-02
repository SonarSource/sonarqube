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
import { ONE_SECOND } from '../../helpers/constants';
import { toShortISO8601String } from '../../helpers/dates';
import { ActivityRequestParameters, Task, TaskStatuses } from '../../types/tasks';
import { ALL_TYPES, CURRENTS, STATUSES } from './constants';

export interface Query {
  currents: string;
  maxExecutedAt?: Date;
  minSubmittedAt?: Date;
  query: string;
  status: string;
  taskType: string;
}

export function updateTask(tasks: Task[], newTask: Task) {
  return tasks.map((task) => (task.id === newTask.id ? newTask : task));
}

export function mapFiltersToParameters(filters: Partial<Query> = {}) {
  const parameters: ActivityRequestParameters = {};

  if (filters.status === STATUSES.ALL) {
    parameters.status = [
      TaskStatuses.Pending,
      TaskStatuses.InProgress,
      TaskStatuses.Success,
      TaskStatuses.Failed,
      TaskStatuses.Canceled,
    ].join();
  } else if (filters.status === STATUSES.ALL_EXCEPT_PENDING) {
    parameters.status = [
      TaskStatuses.InProgress,
      TaskStatuses.Success,
      TaskStatuses.Failed,
      TaskStatuses.Canceled,
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
    parameters.minSubmittedAt = toShortISO8601String(filters.minSubmittedAt);
  }

  if (filters.maxExecutedAt) {
    parameters.maxExecutedAt = toShortISO8601String(filters.maxExecutedAt);
  }

  if (filters.query) {
    parameters.q = filters.query;
  }

  return parameters;
}

const ONE_MINUTE = 60 * ONE_SECOND;
const ONE_HOUR = 60 * ONE_MINUTE;

function format(int: number, suffix: string) {
  return `${int}${suffix}`;
}

export function formatDuration(value: number | undefined) {
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
