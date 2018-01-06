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
import { RECEIVE_PROJECT_MODULES, CHANGE_KEY } from './actions';

const modulesByProject = (state = {}, action = {}) => {
  if (action.type === RECEIVE_PROJECT_MODULES) {
    const moduleKeys = action.modules.map(module => module.key);
    return { ...state, [action.projectKey]: moduleKeys };
  }

  if (action.type === CHANGE_KEY) {
    const newState = {};
    Object.keys(state).forEach(projectKey => {
      const moduleKeys = state[projectKey];
      const changedKeyIndex = moduleKeys.indexOf(action.key);
      if (changedKeyIndex !== -1) {
        const newModuleKeys = [...moduleKeys];
        newModuleKeys.splice(changedKeyIndex, 1, action.newKey);
        newState[projectKey] = newModuleKeys;
      } else {
        newState[projectKey] = moduleKeys;
      }
    });
    return newState;
  }

  return state;
};

export default modulesByProject;

export const getProjectModules = (state, projectKey) => state[projectKey];
