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
import _ from 'underscore';
import { getTypes, getActivity, cancelAllTasks, cancelTask as cancelTaskAPI } from '../../../api/ce';

import { STATUSES, ALL_TYPES, CURRENTS, DEBOUNCE_DELAY } from '../constants';

const PAGE_SIZE = 1000;

export const INIT = 'INIT';
export const REQUEST_TASKS = 'REQUEST_TASKS';
export const RECEIVE_TASKS = 'RECEIVE_TASKS';
export const UPDATE_QUERY = 'UPDATE_QUERY';
export const RECEIVE_STATS = 'RECEIVE_STATS';
export const CANCEL_ALL_PENDING = 'CANCEL_ALL_PENDING';
export const CANCEL_TASK = 'CANCEL_TASK';
export const FINISH_CANCEL_TASK = 'FINISH_CANCEL_TASK';

export function init (component, types) {
  return {
    type: INIT,
    component,
    types
  };
}

export function requestTasks (filters) {
  return {
    type: REQUEST_TASKS,
    ...filters
  };
}

export function receiveTasks (tasks) {
  return {
    type: RECEIVE_TASKS,
    tasks
  };
}

export function updateQuery (query) {
  return {
    type: UPDATE_QUERY,
    query
  };
}

export function receiveStats ({ pendingCount, failingCount }) {
  return {
    type: RECEIVE_STATS,
    pendingCount,
    failingCount
  };
}

export function cancelAllPendingAction () {
  return {
    type: CANCEL_ALL_PENDING
  };
}

export function cancelTaskAction (task) {
  return {
    type: CANCEL_TASK,
    task
  };
}

export function finishCancelTaskAction (task) {
  return {
    type: FINISH_CANCEL_TASK,
    task
  };
}

function mapFiltersToParameters (filters = {}) {
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

  if (filters.date.minSubmittedAt) {
    parameters.minSubmittedAt = filters.date.minSubmittedAt;
  }

  if (filters.date.maxExecutedAt) {
    parameters.maxExecutedAt = filters.date.maxExecutedAt;
  }

  if (filters.query) {
    parameters.componentQuery = filters.query;
  }

  if (filters.lastPage !== 1) {
    parameters.p = filters.lastPage;
  }

  return parameters;
}

function fetchTasks (filters) {
  return (dispatch, getState) => {
    const { component } = getState();
    const parameters = mapFiltersToParameters(filters);

    if (component) {
      parameters.componentId = component.id;
    }

    dispatch(requestTasks(filters));

    return Promise.all([
      getActivity(parameters)
    ]).then(responses => {
      const [activity] = responses;
      const tasks = activity.tasks;

      dispatch(receiveTasks(tasks));

      // FIXME request real numbers
      const pendingCount = 0;
      const failingCount = 0;

      dispatch(receiveStats({ pendingCount, failingCount }));
    });
  };
}

export function filterTasks (overriddenFilters) {
  return (dispatch, getState) => {
    const { status, taskType, currents, date, query } = getState();
    const filters = { status, taskType, currents, date, query };
    const finalFilters = { ...filters, ...overriddenFilters };

    dispatch(fetchTasks(finalFilters));
  };
}

const debouncedFilter = _.debounce((dispatch, overriddenFilters) => {
  dispatch(filterTasks(overriddenFilters));
}, DEBOUNCE_DELAY);

export function search (query) {
  return dispatch => {
    dispatch(updateQuery(query));
    debouncedFilter(dispatch, { query });
  };
}

export function cancelAllPending () {
  return dispatch => {
    dispatch(cancelAllPendingAction());
    cancelAllTasks().then(() => dispatch(filterTasks()));
  };
}

export function cancelTask (task) {
  return dispatch => {
    dispatch(cancelTaskAction(task));
    cancelTaskAPI(task.id).then(nextTask => dispatch(finishCancelTaskAction(nextTask)));
  };
}

export function initApp (component) {
  return dispatch => {
    getTypes().then(types => {
      dispatch(init(component, types));
      dispatch(filterTasks());
    });
  };
}
