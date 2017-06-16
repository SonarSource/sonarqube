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
import { getLanguages } from '../api/languages';
import { getGlobalNavigation, getComponentNavigation } from '../api/nav';
import { getComponentData } from '../api/components';
import * as auth from '../api/auth';
import { getOrganizations } from '../api/organizations';
import { receiveLanguages } from './languages/actions';
import { receiveComponents } from './components/actions';
import { addGlobalErrorMessage } from './globalMessages/duck';
import { parseError } from '../apps/code/utils';
import { setAppState } from './appState/duck';
import { receiveOrganizations } from './organizations/duck';

export const onFail = dispatch => error =>
  parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));

export const fetchAppState = () => dispatch =>
  getGlobalNavigation().then(appState => dispatch(setAppState(appState)), onFail(dispatch));

export const fetchLanguages = () => dispatch => {
  return getLanguages().then(languages => dispatch(receiveLanguages(languages)), onFail(dispatch));
};

export const fetchOrganizations = (organizations?: Array<string>) => dispatch =>
  getOrganizations(organizations).then(
    r => dispatch(receiveOrganizations(r.organizations)),
    onFail(dispatch)
  );

const addQualifier = project => ({
  ...project,
  qualifier: project.breadcrumbs[project.breadcrumbs.length - 1].qualifier
});

export const fetchProject = key => dispatch =>
  Promise.all([
    getComponentNavigation(key),
    getComponentData(key)
  ]).then(([componentNav, componentData]) => {
    const component = { ...componentData, ...componentNav };
    dispatch(receiveComponents([addQualifier(component)]));
    if (component.organization != null) {
      dispatch(fetchOrganizations([component.organization]));
    }
  });

export const doLogin = (login, password) => dispatch =>
  auth.login(login, password).then(
    () => {
      /* everything is fine */
    },
    () => {
      dispatch(addGlobalErrorMessage('Authentication failed'));
      return Promise.reject();
    }
  );

export const doLogout = () => dispatch =>
  auth.logout().then(
    () => {
      /* everything is fine */
    },
    () => {
      dispatch(addGlobalErrorMessage('Logout failed'));
      return Promise.reject();
    }
  );
