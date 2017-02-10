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
import keyBy from 'lodash/keyBy';
import type {
  Action,
  ReceiveProjectActivityAction,
  AddEventAction,
  DeleteEventAction,
  ChangeEventAction
} from './duck';

export type State = {
  [key: string]: {
    key: string,
    name: string,
    category: string,
    description?: string,
  }
};

const receiveProjectActivity = (state: State, action: ReceiveProjectActivityAction): State => {
  const events = {};
  action.analyses.forEach(analysis => {
    Object.assign(events, keyBy(analysis.events, 'key'));
  });
  return { ...state, ...events };
};

const addEvent = (state: State, action: AddEventAction): State => {
  return { ...state, [action.event.key]: action.event };
};

const deleteEvent = (state: State, action: DeleteEventAction): State => {
  const newState = { ...state };
  delete newState[action.event];
  return newState;
};

const changeEvent = (state: State, action: ChangeEventAction): State => {
  const newEvent = { ...state[action.event], ...action.changes };
  return { ...state, [action.event]: newEvent };
};

export default (state: State = {}, action: Action): State => {
  switch (action.type) {
    case 'RECEIVE_PROJECT_ACTIVITY':
      return receiveProjectActivity(state, action);
    case 'ADD_PROJECT_ACTIVITY_EVENT':
      return addEvent(state, action);
    case 'DELETE_PROJECT_ACTIVITY_EVENT':
      return deleteEvent(state, action);
    case 'CHANGE_PROJECT_ACTIVITY_EVENT':
      return changeEvent(state, action);
    default:
      return state;
  }
};

export const getEvent = (state: State, key: string) => (
    state[key]
);
