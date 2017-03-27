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
import { keyBy } from 'lodash';
import type {
  Action,
  ReceiveProjectActivityAction,
  AddEventAction,
  DeleteEventAction,
  DeleteAnalysisAction
} from './duck';

type Analysis = {
  key: string,
  date: string,
  events: Array<string>
};

export type State = {
  [key: string]: Analysis
};

const receiveProjectActivity = (state: State, action: ReceiveProjectActivityAction): State => {
  const analysesWithFlatEvents = action.analyses.map(analysis => ({
    ...analysis,
    events: analysis.events.map(event => event.key)
  }));
  return { ...state, ...keyBy(analysesWithFlatEvents, 'key') };
};

const addEvent = (state: State, action: AddEventAction): State => {
  const analysis = state[action.analysis];
  const newAnalysis = {
    ...analysis,
    events: [...analysis.events, action.event.key]
  };
  return { ...state, [action.analysis]: newAnalysis };
};

const deleteEvent = (state: State, action: DeleteEventAction): State => {
  const analysis = state[action.analysis];
  const newAnalysis = {
    ...analysis,
    events: analysis.events.filter(event => event !== action.event)
  };
  return { ...state, [action.analysis]: newAnalysis };
};

const deleteAnalysis = (state: State, action: DeleteAnalysisAction): State => {
  const newState = { ...state };
  delete newState[action.analysis];
  return newState;
};

export default (state: State = {}, action: Action): State => {
  switch (action.type) {
    case 'RECEIVE_PROJECT_ACTIVITY':
      return receiveProjectActivity(state, action);
    case 'ADD_PROJECT_ACTIVITY_EVENT':
      return addEvent(state, action);
    case 'DELETE_PROJECT_ACTIVITY_EVENT':
      return deleteEvent(state, action);
    case 'DELETE_PROJECT_ACTIVITY_ANALYSIS':
      return deleteAnalysis(state, action);
    default:
      return state;
  }
};

export const getAnalysis = (state: State, key: string): Analysis => state[key];
