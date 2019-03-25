/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { omit } from 'lodash';
import { combineReducers } from 'redux';
import { ActionType } from '../../../store/utils/actions';

const enum Actions {
  CancelChange = 'settingsPage/CANCEL_CHANGE',
  ChangeValue = 'settingsPage/CHANGE_VALUE',
  FailValidation = 'settingsPage/FAIL_VALIDATION',
  PassValidation = 'settingsPage/PASS_VALIDATION',
  StartLoading = 'settingsPage/START_LOADING',
  StopLoading = 'settingsPage/STOP_LOADING'
}

type Action =
  | ActionType<typeof cancelChange, Actions.CancelChange>
  | ActionType<typeof changeValue, Actions.ChangeValue>
  | ActionType<typeof failValidation, Actions.FailValidation>
  | ActionType<typeof passValidation, Actions.PassValidation>
  | ActionType<typeof startLoading, Actions.StartLoading>
  | ActionType<typeof stopLoading, Actions.StopLoading>;

export interface State {
  changedValues: T.Dict<any>;
  loading: T.Dict<boolean>;
  validationMessages: T.Dict<string>;
}

export function cancelChange(key: string) {
  return { type: Actions.CancelChange, key };
}

export function changeValue(key: string, value: any) {
  return { type: Actions.ChangeValue, key, value };
}

function changedValues(state: State['changedValues'] = {}, action: Action) {
  if (action.type === Actions.ChangeValue) {
    return { ...state, [action.key]: action.value };
  }
  if (action.type === Actions.CancelChange) {
    return omit(state, action.key);
  }
  return state;
}

export function failValidation(key: string, message: string) {
  return { type: Actions.FailValidation, key, message };
}

export function passValidation(key: string) {
  return { type: Actions.PassValidation, key };
}

function validationMessages(state: State['validationMessages'] = {}, action: Action) {
  if (action.type === Actions.FailValidation) {
    return { ...state, [action.key]: action.message };
  }
  if (action.type === Actions.PassValidation) {
    return omit(state, action.key);
  }
  return state;
}

export function startLoading(key: string) {
  return { type: Actions.StartLoading, key };
}

export function stopLoading(key: string) {
  return { type: Actions.StopLoading, key };
}

function loading(state: State['loading'] = {}, action: Action) {
  if (action.type === Actions.StartLoading) {
    return { ...state, [action.key]: true };
  }
  if (action.type === Actions.StopLoading) {
    return { ...state, [action.key]: false };
  }
  return state;
}

export default combineReducers({ changedValues, loading, validationMessages });

export function getChangedValue(state: State, key: string) {
  return state.changedValues[key];
}

export function getValidationMessage(state: State, key: string): string | undefined {
  return state.validationMessages[key];
}

export function isLoading(state: State, key: string) {
  return Boolean(state.loading[key]);
}
