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
import { FAIL_VALIDATION, PASS_VALIDATION } from './actions';

const reducer = (state = {}, action = {}) => {
  if (action.type === FAIL_VALIDATION) {
    return { ...state, [action.key]: action.message };
  }

  if (action.type === PASS_VALIDATION) {
    return { ...state, [action.key]: null };
  }

  return state;
};

export default reducer;

export const getValidationMessage = (state, key) => state[key];
