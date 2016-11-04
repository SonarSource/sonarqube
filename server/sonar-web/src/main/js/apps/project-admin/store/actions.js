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
import { getQualityProfiles, associateProject, dissociateProject } from '../../../api/quality-profiles';
import {
  fetchQualityGates,
  getGateForProject,
  associateGateWithProject,
  dissociateGateWithProject
} from '../../../api/quality-gates';
import { getProjectLinks, createLink } from '../../../api/projectLinks';
import { getTree, changeKey as changeKeyApi } from '../../../api/components';
import { addGlobalSuccessMessage } from '../../../components/store/globalMessages';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectAdminProfileByKey } from '../../../app/store/rootReducer';

export const RECEIVE_PROFILES = 'projectAdmin/RECEIVE_PROFILES';
export const receiveProfiles = profiles => ({
  type: RECEIVE_PROFILES,
  profiles
});

export const RECEIVE_PROJECT_PROFILES = 'projectAdmin/RECEIVE_PROJECT_PROFILES';
export const receiveProjectProfiles = (projectKey, profiles) => ({
  type: RECEIVE_PROJECT_PROFILES,
  projectKey,
  profiles
});

export const fetchProjectProfiles = projectKey => dispatch => {
  Promise.all([
    getQualityProfiles(),
    getQualityProfiles({ projectKey })
  ]).then(responses => {
    const [allProfiles, projectProfiles] = responses;
    dispatch(receiveProfiles(allProfiles));
    dispatch(receiveProjectProfiles(projectKey, projectProfiles));
  });
};

export const SET_PROJECT_PROFILE = 'projectAdmin/SET_PROJECT_PROFILE';
const setProjectProfileAction = (projectKey, oldProfileKey, newProfileKey) => ({
  type: SET_PROJECT_PROFILE,
  projectKey,
  oldProfileKey,
  newProfileKey
});

export const setProjectProfile = (projectKey, oldKey, newKey) =>
    (dispatch, getState) => {
      const state = getState();
      const newProfile = getProjectAdminProfileByKey(state, newKey);
      const request = newProfile.isDefault ?
          dissociateProject(oldKey, projectKey) :
          associateProject(newKey, projectKey);

      request.then(() => {
        dispatch(setProjectProfileAction(projectKey, oldKey, newKey));
        dispatch(addGlobalSuccessMessage(
            translateWithParameters(
                'project_quality_profile.successfully_updated',
                newProfile.languageName)));
      });
    };

export const RECEIVE_GATES = 'projectAdmin/RECEIVE_GATES';
export const receiveGates = gates => ({
  type: RECEIVE_GATES,
  gates
});

export const RECEIVE_PROJECT_GATE = 'projectAdmin/RECEIVE_PROJECT_GATE';
export const receiveProjectGate = (projectKey, gate) => ({
  type: RECEIVE_PROJECT_GATE,
  projectKey,
  gate
});

export const fetchProjectGate = projectKey => dispatch => {
  Promise.all([
    fetchQualityGates(),
    getGateForProject(projectKey)
  ]).then(responses => {
    const [allGates, projectGate] = responses;
    dispatch(receiveGates(allGates));
    dispatch(receiveProjectGate(projectKey, projectGate));
  });
};

export const SET_PROJECT_GATE = 'projectAdmin/SET_PROJECT_GATE';
const setProjectGateAction = (projectKey, gateId) => ({
  type: SET_PROJECT_GATE,
  projectKey,
  gateId
});

export const setProjectGate = (projectKey, oldId, newId) => dispatch => {
  const request = newId != null ?
      associateGateWithProject(newId, projectKey) :
      dissociateGateWithProject(oldId, projectKey);

  request.then(() => {
    dispatch(setProjectGateAction(projectKey, newId));
    dispatch(addGlobalSuccessMessage(
        translate('project_quality_gate.successfully_updated')));
  });
};

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
  return changeKeyApi(key, newKey)
      .then(() => dispatch(changeKeyAction(key, newKey)));
};
