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
import {
  SET_STATE,
  ADD,
  DELETE,
  SHOW,
  RENAME,
  COPY,
  SET_AS_DEFAULT,
  ADD_CONDITION,
  DELETE_CONDITION,
  SAVE_CONDITION
} from './actions';
import { checkIfDefault, addCondition, deleteCondition, replaceCondition } from './utils';

const initialState = {};

export default function rootReducer(state = initialState, action = {}) {
  switch (action.type) {
    case SET_STATE:
      return { ...state, ...action.nextState };
    case ADD:
    case COPY:
      return { ...state, qualityGates: [...state.qualityGates, action.qualityGate] };
    case DELETE:
      return {
        ...state,
        qualityGates: state.qualityGates.filter(candidate => candidate.id !== action.qualityGate.id)
      };
    case SHOW:
      return {
        ...state,
        qualityGate: {
          ...action.qualityGate,
          isDefault: checkIfDefault(action.qualityGate, state.qualityGates)
        }
      };
    case RENAME:
      return {
        ...state,
        qualityGates: state.qualityGates.map(candidate => {
          return candidate.id === action.qualityGate.id
            ? { ...candidate, name: action.newName }
            : candidate;
        }),
        qualityGate: { ...state.qualityGate, name: action.newName }
      };
    case SET_AS_DEFAULT:
      return {
        ...state,
        qualityGates: state.qualityGates.map(candidate => {
          return { ...candidate, isDefault: candidate.id === action.qualityGate.id };
        }),
        qualityGate: {
          ...action.qualityGate,
          isDefault: state.qualityGate.id === action.qualityGate.id
        }
      };
    case ADD_CONDITION:
      return {
        ...state,
        qualityGate: addCondition(state.qualityGate, action.metric)
      };
    case DELETE_CONDITION:
      return {
        ...state,
        qualityGate: deleteCondition(state.qualityGate, action.condition)
      };
    case SAVE_CONDITION:
      return {
        ...state,
        qualityGate: replaceCondition(state.qualityGate, action.oldCondition, action.newCondition)
      };
    default:
      return state;
  }
}
