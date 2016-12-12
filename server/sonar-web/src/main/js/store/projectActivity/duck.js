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
// @flow
import { combineReducers } from 'redux';
import analyses from './analyses';
import paging from './paging';

export type Event = {|
  key: string,
  name: string;
  category: string;
  description?: string;
|};

export type Analysis = {
  key: string;
  date: string;
  events: Array<Event>
};

export type Paging = {
  total: number,
  pageIndex: number,
  pageSize: number
};

export type ReceiveProjectActivityAction = {
  type: 'RECEIVE_PROJECT_ACTIVITY',
  project: string,
  analyses: Array<Analysis>,
  paging: Paging
};

export const receiveProjectActivity = (
    project: string,
    analyses: Array<Analysis>,
    paging: Paging
): ReceiveProjectActivityAction => ({
  type: 'RECEIVE_PROJECT_ACTIVITY',
  project,
  analyses,
  paging
});

const byProject = combineReducers({ analyses, paging });

type State = {
  [key: string]: {
    analyses: Array<Analysis>,
    paging: Paging
  }
};

type Action = ReceiveProjectActivityAction;

const reducer = (state: State = {}, action: Action): State => {
  if (action.type === 'RECEIVE_PROJECT_ACTIVITY') {
    return { ...state, [action.project]: byProject(state[action.project], action) };
  }
  return state;
};

export default reducer;

export const getAnalyses = (state: State, project: string) => (
    state[project] && state[project].analyses
);

export const getPaging = (state: State, project: string) => (
    state[project] && state[project].paging
);
