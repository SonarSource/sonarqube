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
import filter from './filter';
import paging from './paging';

export type Event = {
  key: string,
  name: string;
  category: string;
  description?: string;
};

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

export type ChangeProjectActivityFilter = {
  type: 'CHANGE_PROJECT_ACTIVITY_FILTER',
  project: string,
  filter: ?string
};

export type AddEventAction = {
  type: 'ADD_PROJECT_ACTIVITY_EVENT',
  project: string,
  analysis: string,
  event: Event
};

export type DeleteEventAction = {
  type: 'DELETE_PROJECT_ACTIVITY_EVENT',
  project: string,
  analysis: string,
  event: string
};

export type ChangeEventAction = {
  type: 'CHANGE_PROJECT_ACTIVITY_EVENT',
  project: string,
  analysis: string,
  event: string,
  changes: Object
};

export type DeleteAnalysisAction = {
  type: 'DELETE_PROJECT_ACTIVITY_ANALYSIS',
  project: string,
  analysis: string
};

export type Action =
    ReceiveProjectActivityAction |
        ChangeProjectActivityFilter |
        AddEventAction |
        DeleteEventAction |
        ChangeEventAction |
        DeleteAnalysisAction;

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

export const changeProjectActivityFilter = (project: string, filter: ?string): ChangeProjectActivityFilter => ({
  type: 'CHANGE_PROJECT_ACTIVITY_FILTER',
  project,
  filter
});

export const addEvent = (project: string, analysis: string, event: Event): AddEventAction => ({
  type: 'ADD_PROJECT_ACTIVITY_EVENT',
  project,
  analysis,
  event
});

export const deleteEvent = (project: string, analysis: string, event: string): DeleteEventAction => ({
  type: 'DELETE_PROJECT_ACTIVITY_EVENT',
  project,
  analysis,
  event
});

export const changeEvent = (project: string, analysis: string, event: string, changes: Object): ChangeEventAction => ({
  type: 'CHANGE_PROJECT_ACTIVITY_EVENT',
  project,
  analysis,
  event,
  changes
});

export const deleteAnalysis = (project: string, analysis: string): DeleteAnalysisAction => ({
  type: 'DELETE_PROJECT_ACTIVITY_ANALYSIS',
  project,
  analysis
});

const byProject = combineReducers({ analyses, filter, paging });

type State = {
  [key: string]: {
    analyses: Array<Analysis>,
    filter: ?string,
    paging: Paging
  }
};

const reducer = (state: State = {}, action: Action): State => {
  const actions = [
    'RECEIVE_PROJECT_ACTIVITY',
    'CHANGE_PROJECT_ACTIVITY_FILTER',
    'ADD_PROJECT_ACTIVITY_EVENT',
    'DELETE_PROJECT_ACTIVITY_EVENT',
    'CHANGE_PROJECT_ACTIVITY_EVENT',
    'DELETE_PROJECT_ACTIVITY_ANALYSIS'
  ];

  if (actions.includes(action.type)) {
    return { ...state, [action.project]: byProject(state[action.project], action) };
  }
  return state;
};

export default reducer;

export const getAnalyses = (state: State, project: string) => (
    state[project] && state[project].analyses
);

export const getFilter = (state: State, project: string) => (
    state[project] && state[project].filter
);


export const getPaging = (state: State, project: string) => (
    state[project] && state[project].paging
);
