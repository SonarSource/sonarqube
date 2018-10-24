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
import { Dispatch } from 'redux';
import { addGlobalErrorMessage } from './globalMessages';
import { receiveLanguages } from './languages';
import { receiveMetrics } from './metrics';
import { receiveOrganizations } from './organizations';
import * as auth from '../api/auth';
import { getLanguages } from '../api/languages';
import { getAllMetrics } from '../api/metrics';
import { getOrganizations } from '../api/organizations';

export function fetchLanguages() {
  return (dispatch: Dispatch) => {
    getLanguages().then(languages => dispatch(receiveLanguages(languages)), () => {});
  };
}

export function fetchMetrics() {
  return (dispatch: Dispatch) => {
    getAllMetrics().then(metrics => dispatch(receiveMetrics(metrics)), () => {});
  };
}

export function fetchOrganizations(organizations: string[]) {
  return (dispatch: Dispatch) => {
    getOrganizations({ organizations: organizations && organizations.join() }).then(
      r => dispatch(receiveOrganizations(r.organizations)),
      () => {}
    );
  };
}

export function doLogin(login: string, password: string) {
  return (dispatch: Dispatch<any>) =>
    auth.login(login, password).then(
      () => {
        /* everything is fine */
      },
      () => {
        dispatch(addGlobalErrorMessage('Authentication failed'));
        return Promise.reject();
      }
    );
}

export function doLogout() {
  return (dispatch: Dispatch<any>) =>
    auth.logout().then(
      () => {
        /* everything is fine */
      },
      () => {
        dispatch(addGlobalErrorMessage('Logout failed'));
        return Promise.reject();
      }
    );
}
