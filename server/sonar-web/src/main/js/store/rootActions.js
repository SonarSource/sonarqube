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
import { setAppState } from './appState/duck';
import { receiveOrganizations } from './organizations/duck';
import { receiveLanguages } from './languages/actions';
import { receiveMetrics } from './metrics/actions';
import { addGlobalErrorMessage } from './globalMessages/duck';
import { getLanguages } from '../api/languages';
import { getGlobalNavigation } from '../api/nav';
import * as auth from '../api/auth';
import { getOrganizations } from '../api/organizations';
import { getAllMetrics } from '../api/metrics';
import { parseError } from '../helpers/request';

export const onFail = dispatch => error =>
  parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));

export const fetchAppState = () => dispatch =>
  getGlobalNavigation().then(appState => {
    dispatch(setAppState(appState));
    return appState;
  }, onFail(dispatch));

export const fetchLanguages = () => dispatch =>
  getLanguages().then(languages => dispatch(receiveLanguages(languages)), onFail(dispatch));

export const fetchMetrics = () => dispatch =>
  getAllMetrics().then(metrics => dispatch(receiveMetrics(metrics)), onFail(dispatch));

export const fetchOrganizations = (organizations /*: Array<string> | void */) => dispatch =>
  getOrganizations({ organizations: organizations && organizations.join() }).then(
    r => dispatch(receiveOrganizations(r.organizations)),
    onFail(dispatch)
  );

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
