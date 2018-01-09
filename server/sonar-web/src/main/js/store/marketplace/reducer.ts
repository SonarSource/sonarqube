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
import { Action } from './actions';
import { Edition, EditionStatus } from '../../api/marketplace';

interface State {
  editions?: Edition[];
  loading: boolean;
  status?: EditionStatus;
  readOnly: boolean;
}

const defaultState: State = {
  loading: true,
  readOnly: false
};

export default function(state: State = defaultState, action: Action): State {
  if (action.type === 'SET_EDITIONS') {
    return { ...state, editions: action.editions, readOnly: action.readOnly, loading: false };
  }
  if (action.type === 'LOAD_EDITIONS') {
    return { ...state, loading: action.loading };
  }
  if (action.type === 'SET_EDITION_STATUS') {
    const hasChanged = Object.keys(action.status).some(
      (key: keyof EditionStatus) => !state.status || state.status[key] !== action.status[key]
    );
    // Prevent from rerendering the whole admin if the status didn't change
    if (hasChanged) {
      return { ...state, status: action.status };
    }
  }
  return state;
}

export const getEditions = (state: State) => state.editions;
export const getEditionStatus = (state: State) => state.status;
