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
import { START_LOADING, FAIL_VALIDATION, PASS_VALIDATION } from './actions';

const singleSettingReducer = (state = {}, action = {}) => {
  if (action.type === START_LOADING) {
    return { ...state, key: action.key, loading: true };
  }

  if (action.type === FAIL_VALIDATION) {
    return { ...state, key: action.key, loading: false, validationMessage: action.message };
  }

  if (action.type === PASS_VALIDATION) {
    return { ...state, key: action.key, loading: false, validationMessage: null };
  }

  return state;
};

const reducer = (state = {}, action = {}) => {
  if ([START_LOADING, FAIL_VALIDATION, PASS_VALIDATION].includes(action.type)) {
    const newItem = singleSettingReducer(state[action.key], action);
    return { ...state, [action.key]: newItem };
  }

  return state;
};

export default reducer;

export const isLoading = (state, key) => state[key] ? state[key].loading : false;

export const getValidationMessage = (state, key) => state[key] ? state[key].validationMessage : null;
