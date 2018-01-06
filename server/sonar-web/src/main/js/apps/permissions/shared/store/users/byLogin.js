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
import { keyBy } from 'lodash';
import {
  RECEIVE_HOLDERS_SUCCESS,
  GRANT_PERMISSION_TO_USER,
  REVOKE_PERMISSION_TO_USER
} from '../actions';

const byLogin = (state = {}, action = {}) => {
  if (action.type === RECEIVE_HOLDERS_SUCCESS) {
    const newUsers = keyBy(action.users, 'login');
    return { ...state, ...newUsers };
  } else if (action.type === GRANT_PERMISSION_TO_USER) {
    const newUser = { ...state[action.login] };
    newUser.permissions = [...newUser.permissions, action.permission];
    return { ...state, [action.login]: newUser };
  } else if (action.type === REVOKE_PERMISSION_TO_USER) {
    const newUser = { ...state[action.login] };
    newUser.permissions = newUser.permissions.filter(p => p !== action.permission);
    return { ...state, [action.login]: newUser };
  } else {
    return state;
  }
};

export default byLogin;

export const getUsers = state => state;

export const getUserByLogin = (state, login) => state[login];
