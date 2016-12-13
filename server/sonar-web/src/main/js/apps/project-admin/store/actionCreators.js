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
// @flow
export const RECEIVE_PROFILES = 'projectAdmin/RECEIVE_PROFILES';
export const RECEIVE_PROJECT_PROFILES = 'projectAdmin/RECEIVE_PROJECT_PROFILES';
export const SET_PROJECT_PROFILE = 'projectAdmin/SET_PROJECT_PROFILE';
export const RECEIVE_GATES = 'projectAdmin/RECEIVE_GATES';
export const RECEIVE_PROJECT_GATE = 'projectAdmin/RECEIVE_PROJECT_GATE';
export const SET_PROJECT_GATE = 'projectAdmin/SET_PROJECT_GATE';
export const RECEIVE_PROJECT_LINKS = 'projectAdmin/RECEIVE_PROJECT_LINKS';
export const ADD_PROJECT_LINK = 'projectAdmin/ADD_PROJECT_LINK';
export const DELETE_PROJECT_LINK = 'projectAdmin/DELETE_PROJECT_LINK';
export const RECEIVE_PROJECT_MODULES = 'projectAdmin/RECEIVE_PROJECT_MODULES';
export const CHANGE_KEY = 'projectAdmin/CHANGE_KEY';

export const receiveProfiles = (profiles: Array<*>) => ({
  type: RECEIVE_PROFILES,
  profiles
});


export const receiveProjectProfiles = (projectKey: string, profiles: Array<Object>) => ({
  type: RECEIVE_PROJECT_PROFILES,
  projectKey,
  profiles
});

export const setProjectProfileAction = (projectKey: string, oldProfileKey: string, newProfileKey: string) => ({
  type: SET_PROJECT_PROFILE,
  projectKey,
  oldProfileKey,
  newProfileKey
});

export const receiveGates = (gates: Array<Object>) => ({
  type: RECEIVE_GATES,
  gates
});

export const receiveProjectGate = (projectKey: string, gate: Object) => ({
  type: RECEIVE_PROJECT_GATE,
  projectKey,
  gate
});

export const setProjectGateAction = (projectKey: string, gateId: string) => ({
  type: SET_PROJECT_GATE,
  projectKey,
  gateId
});

export const receiveProjectLinks = (projectKey: string, links: Array<Object>) => ({
  type: RECEIVE_PROJECT_LINKS,
  projectKey,
  links
});

export const addProjectLink = (projectKey: string, link: Object) => ({
  type: ADD_PROJECT_LINK,
  projectKey,
  link
});

export const deleteProjectLink = (projectKey: string, linkId: string) => ({
  type: DELETE_PROJECT_LINK,
  projectKey,
  linkId
});

export const receiveProjectModules = (projectKey: string, modules: Array<Object>) => ({
  type: RECEIVE_PROJECT_MODULES,
  projectKey,
  modules
});

export const changeKeyAction = (key: string, newKey: string) => ({
  type: CHANGE_KEY,
  key,
  newKey
});
