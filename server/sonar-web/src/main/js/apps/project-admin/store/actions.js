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
import { getProjectLinks, createLink } from '../../../api/projectLinks';
import { getTree, changeKey as changeKeyApi } from '../../../api/components';

export const RECEIVE_PROJECT_LINKS = 'projectAdmin/RECEIVE_PROJECT_LINKS';
export const receiveProjectLinks = (projectKey, links) => ({
  type: RECEIVE_PROJECT_LINKS,
  projectKey,
  links
});

export const fetchProjectLinks = projectKey => dispatch => {
  getProjectLinks(projectKey).then(links => {
    dispatch(receiveProjectLinks(projectKey, links));
  });
};

export const ADD_PROJECT_LINK = 'projectAdmin/ADD_PROJECT_LINK';
const addProjectLink = (projectKey, link) => ({
  type: ADD_PROJECT_LINK,
  projectKey,
  link
});

export const createProjectLink = (projectKey, name, url) => dispatch => {
  return createLink(projectKey, name, url).then(link => {
    dispatch(addProjectLink(projectKey, link));
  });
};

export const DELETE_PROJECT_LINK = 'projectAdmin/DELETE_PROJECT_LINK';
export const deleteProjectLink = (projectKey, linkId) => ({
  type: DELETE_PROJECT_LINK,
  projectKey,
  linkId
});

export const RECEIVE_PROJECT_MODULES = 'projectAdmin/RECEIVE_PROJECT_MODULES';
const receiveProjectModules = (projectKey, modules) => ({
  type: RECEIVE_PROJECT_MODULES,
  projectKey,
  modules
});

export const fetchProjectModules = projectKey => dispatch => {
  const options = { qualifiers: 'BRC', s: 'name', ps: 500 };
  getTree(projectKey, options).then(r => {
    dispatch(receiveProjectModules(projectKey, r.components));
  });
};

export const CHANGE_KEY = 'projectAdmin/CHANGE_KEY';
const changeKeyAction = (key, newKey) => ({
  type: CHANGE_KEY,
  key,
  newKey
});

export const changeKey = (key, newKey) => dispatch => {
  return changeKeyApi(key, newKey).then(() => dispatch(changeKeyAction(key, newKey)));
};
