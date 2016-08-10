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
import { RECEIVE_PROJECT_GATE, SET_PROJECT_GATE } from './actions';

const gateByProject = (state = {}, action = {}) => {
  if (action.type === RECEIVE_PROJECT_GATE) {
    const gateId = action.gate ? action.gate.id : null;
    return { ...state, [action.projectKey]: gateId };
  }

  if (action.type === SET_PROJECT_GATE) {
    return { ...state, [action.projectKey]: action.gateId };
  }

  return state;
};

export default gateByProject;

export const getProjectGate = (state, projectKey) =>
    state[projectKey];
