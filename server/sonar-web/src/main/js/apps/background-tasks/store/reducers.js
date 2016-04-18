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
import {
    INIT,
    REQUEST_TASKS,
    RECEIVE_TASKS,
    UPDATE_QUERY,
    RECEIVE_STATS,
    CANCEL_ALL_PENDING,
    FINISH_CANCEL_TASK
} from './actions';
import { DEFAULT_FILTERS } from '../constants';

export const initialState = {
  fetching: false,
  tasks: [],

  types: [],

  // filters
  ...DEFAULT_FILTERS,

  // stats
  pendingCount: 0,
  failingCount: 0
};

function updateTask (tasks, newTask) {
  return tasks.map(task => task.id === newTask.id ? newTask : task);
}

export default function (state = initialState, action) {
  switch (action.type) {
    case INIT:
      return {
        ...state,
        component: action.component,
        types: action.types
      };
    case REQUEST_TASKS:
      return {
        ...state,
        fetching: true,
        status: action.status,
        currents: action.currents,
        date: action.date,
        query: action.query,
        taskType: action.taskType
      };
    case RECEIVE_TASKS:
      return {
        ...state,
        fetching: false,
        tasks: action.tasks
      };
    case UPDATE_QUERY:
      return {
        ...state,
        query: action.query
      };
    case RECEIVE_STATS:
      return {
        ...state,
        pendingCount: action.pendingCount,
        failingCount: action.failingCount
      };
    case CANCEL_ALL_PENDING:
      return {
        ...state,
        fetching: true
      };
    case FINISH_CANCEL_TASK:
      return {
        ...state,
        tasks: updateTask(state.tasks, action.task)
      };
    default:
      return state;
  }
}
