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
import * as actionCreators from './actionCreators';
import { getProjectLinks, createLink } from '../../../api/projectLinks';
import { getTree, changeKey as changeKeyApi } from '../../../api/components';
import { addGlobalSuccessMessage } from '../../../store/globalMessages/duck';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectAdminProfileByKey } from '../../../store/rootReducer';

export const fetchProjectProfiles = projectKey => dispatch => {
  Promise.all([
    getQualityProfiles(),
    getQualityProfiles({ projectKey })
  ]).then(responses => {
    const [allProfiles, projectProfiles] = responses;
    dispatch(actionCreators.receiveProfiles(allProfiles));
    dispatch(actionCreators.receiveProjectProfiles(projectKey, projectProfiles));
  });
};

export const setProjectProfile = (projectKey, oldKey, newKey) =>
    (dispatch, getState) => {
      const state = getState();
      const newProfile = getProjectAdminProfileByKey(state, newKey);
      const request = newProfile.isDefault ?
          dissociateProject(oldKey, projectKey) :
          associateProject(newKey, projectKey);

      request.then(() => {
        dispatch(actionCreators.setProjectProfileAction(projectKey, oldKey, newKey));
        dispatch(addGlobalSuccessMessage(
            translateWithParameters(
                'project_quality_profile.successfully_updated',
                newProfile.languageName)));
      });
    };

export const fetchProjectGate = projectKey => dispatch => {
  Promise.all([
    fetchQualityGates(),
    getGateForProject(projectKey)
  ]).then(responses => {
    const [allGates, projectGate] = responses;
    dispatch(actionCreators.receiveGates(allGates));
    dispatch(actionCreators.receiveProjectGate(projectKey, projectGate));
  });
};

export const setProjectGate = (projectKey, oldId, newId) => dispatch => {
  const request = newId != null ?
      associateGateWithProject(newId, projectKey) :
      dissociateGateWithProject(oldId, projectKey);

  request.then(() => {
    dispatch(actionCreators.setProjectGateAction(projectKey, newId));
    dispatch(addGlobalSuccessMessage(
        translate('project_quality_gate.successfully_updated')));
  });
};

export const fetchProjectLinks = projectKey => dispatch => {
  getProjectLinks(projectKey).then(links => {
    dispatch(actionCreators.receiveProjectLinks(projectKey, links));
  });
};

export const createProjectLink = (projectKey, name, url) => dispatch => {
  return createLink(projectKey, name, url).then(link => {
    dispatch(actionCreators.addProjectLink(projectKey, link));
  });
};

export const fetchProjectModules = projectKey => dispatch => {
  const options = { qualifiers: 'BRC', s: 'name', ps: 500 };
  getTree(projectKey, options).then(r => {
    dispatch(actionCreators.receiveProjectModules(projectKey, r.components));
  });
};

export const changeKey = (key, newKey) => dispatch => {
  return changeKeyApi(key, newKey)
      .then(() => dispatch(actionCreators.changeKeyAction(key, newKey)));
};
