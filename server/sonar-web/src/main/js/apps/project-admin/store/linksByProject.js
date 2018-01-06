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
import { without } from 'lodash';
import { RECEIVE_PROJECT_LINKS, DELETE_PROJECT_LINK, ADD_PROJECT_LINK } from './actions';

const linksByProject = (state = {}, action = {}) => {
  if (action.type === RECEIVE_PROJECT_LINKS) {
    const linkIds = action.links.map(link => link.id);
    return { ...state, [action.projectKey]: linkIds };
  }

  if (action.type === ADD_PROJECT_LINK) {
    const byProject = state[action.projectKey] || [];
    const ids = [...byProject, action.link.id];
    return { ...state, [action.projectKey]: ids };
  }

  if (action.type === DELETE_PROJECT_LINK) {
    const ids = without(state[action.projectKey], action.linkId);
    return { ...state, [action.projectKey]: ids };
  }

  return state;
};

export default linksByProject;

export const getLinks = (state, projectKey) => state[projectKey] || [];
