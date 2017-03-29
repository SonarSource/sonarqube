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
import profiles, { getProfile, getAllProfiles as nextGetAllProfiles } from './profiles';
import profilesByProject, { getProfiles } from './profilesByProject';
import gates, { getAllGates as nextGetAllGates, getGate } from './gates';
import gateByProject, { getProjectGate as nextGetProjectGate } from './gateByProject';
import links, { getLink } from './links';
import linksByProject, { getLinks } from './linksByProject';
import components, { getComponentByKey as nextGetComponentByKey } from './components';
import modulesByProject, { getProjectModules as nextGetProjectModules } from './modulesByProject';
import globalMessages, {
  getGlobalMessages as nextGetGlobalMessages
} from '../../../store/globalMessages/duck';

const rootReducer = combineReducers({
  profiles,
  profilesByProject,
  gates,
  gateByProject,
  links,
  linksByProject,
  components,
  modulesByProject,
  globalMessages
});

export default rootReducer;

export const getProfileByKey = (state, profileKey) => getProfile(state.profiles, profileKey);

export const getAllProfiles = state => nextGetAllProfiles(state.profiles);

export const getProjectProfiles = (state, projectKey) =>
  getProfiles(state.profilesByProject, projectKey).map(profileKey =>
    getProfileByKey(state, profileKey));

export const getGateById = (state, gateId) => getGate(state.gates, gateId);

export const getAllGates = state => nextGetAllGates(state.gates);

export const getProjectGate = (state, projectKey) =>
  getGateById(state, nextGetProjectGate(state.gateByProject, projectKey));

export const getLinkById = (state, linkId) => getLink(state.links, linkId);

export const getProjectLinks = (state, projectKey) =>
  getLinks(state.linksByProject, projectKey).map(linkId => getLinkById(state, linkId));

export const getComponentByKey = (state, componentKey) =>
  nextGetComponentByKey(state.components, componentKey);

export const getProjectModules = (state, projectKey) => {
  const moduleKeys = nextGetProjectModules(state.modulesByProject, projectKey);
  if (moduleKeys == null) {
    return null;
  }
  return moduleKeys.map(moduleKey => getComponentByKey(state, moduleKey));
};

export const getGlobalMessages = state => nextGetGlobalMessages(state.globalMessages);
