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
import { combineReducers } from 'redux';
import analyses, * as fromAnalyses from './analyses';
import type { State as AnalysesState } from './analyses';
import analysesByProject from './analysesByProject';
import type { State as AnalysesByProjectState } from './analysesByProject';
import events, * as fromEvents from './events';
import type { State as EventsState } from './events';
import paging from './paging';
import type { State as PagingState } from './paging';

export type Event = {
  key: string,
  name: string,
  category: string,
  description?: string
};

export type Analysis = {
  key: string,
  date: string,
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

export type AddEventAction = {
  type: 'ADD_PROJECT_ACTIVITY_EVENT',
  analysis: string,
  event: Event
};

export type DeleteEventAction = {
  type: 'DELETE_PROJECT_ACTIVITY_EVENT',
  analysis: string,
  event: string
};

export type ChangeEventAction = {
  type: 'CHANGE_PROJECT_ACTIVITY_EVENT',
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

export const addEvent = (analysis: string, event: Event): AddEventAction => ({
  type: 'ADD_PROJECT_ACTIVITY_EVENT',
  analysis,
  event
});

export const deleteEvent = (analysis: string, event: string): DeleteEventAction => ({
  type: 'DELETE_PROJECT_ACTIVITY_EVENT',
  analysis,
  event
});

export const changeEvent = (event: string, changes: Object): ChangeEventAction => ({
  type: 'CHANGE_PROJECT_ACTIVITY_EVENT',
  event,
  changes
});

export const deleteAnalysis = (project: string, analysis: string): DeleteAnalysisAction => ({
  type: 'DELETE_PROJECT_ACTIVITY_ANALYSIS',
  project,
  analysis
});

type State = {
  analyses: AnalysesState,
  analysesByProject: AnalysesByProjectState,
  events: EventsState,
  filter: string,
  paging: PagingState,
};

export default combineReducers({ analyses, analysesByProject, events, paging });

const getEvent = (state: State, key: string): Event => (
    fromEvents.getEvent(state.events, key)
);

const getAnalysis = (state: State, key: string) => {
  const analysis = fromAnalyses.getAnalysis(state.analyses, key);
  const events: Array<Event> = analysis.events.map(key => getEvent(state, key));
  return { ...analysis, events };
};

export const getAnalyses = (state: State, project: string) => (
    state.analysesByProject[project] && state.analysesByProject[project].map(key => getAnalysis(state, key))
);
export const getPaging = (state: State, project: string) => (
    state.paging[project]
);
