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
import { combineReducers } from 'redux';
import uniq from 'lodash/uniq';
import { RECEIVE_CURRENT_USER } from './actions';

const usersByLogin = (state = {}, action = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return { ...state, [action.user.login]: action.user };
  }

  return state;
};

const userLogins = (state = [], action = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return uniq([...state, action.user.login]);
  }

  return state;
};

const currentUser = (state = null, action = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return action.user;
  }

  return state;
};

export default combineReducers({ usersByLogin, userLogins, currentUser });

export const getCurrentUser = state => state.currentUser;
