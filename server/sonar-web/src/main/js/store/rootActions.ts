/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { InjectedRouter } from 'react-router';
import { Dispatch } from 'redux';
import * as auth from '../api/auth';
import { getLanguages } from '../api/languages';
import { getAllMetrics } from '../api/metrics';
import { getQualityGateProjectStatus } from '../api/quality-gates';
import { getBranchLikeQuery } from '../helpers/branch-like';
import { extractStatusConditionsFromProjectStatus } from '../helpers/qualityGates';
import { BranchLike } from '../types/branch-like';
import { requireAuthorization as requireAuthorizationAction } from './appState';
import { registerBranchStatusAction } from './branches';
import { addGlobalErrorMessage } from './globalMessages';
import { receiveLanguages } from './languages';
import { receiveMetrics } from './metrics';

export function fetchLanguages() {
  return (dispatch: Dispatch) => {
    getLanguages().then(
      languages => dispatch(receiveLanguages(languages)),
      () => {
        /* do nothing */
      }
    );
  };
}

export function fetchMetrics() {
  return (dispatch: Dispatch) => {
    getAllMetrics().then(
      metrics => dispatch(receiveMetrics(metrics)),
      () => {
        /* do nothing */
      }
    );
  };
}

export function fetchBranchStatus(branchLike: BranchLike, projectKey: string) {
  return (dispatch: Dispatch<any>) => {
    getQualityGateProjectStatus({ projectKey, ...getBranchLikeQuery(branchLike) }).then(
      projectStatus => {
        const { ignoredConditions, status } = projectStatus;
        const conditions = extractStatusConditionsFromProjectStatus(projectStatus);
        dispatch(
          registerBranchStatusAction(branchLike, projectKey, status, conditions, ignoredConditions)
        );
      },
      () => {
        dispatch(addGlobalErrorMessage('Fetching Quality Gate status failed'));
      }
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

export function requireAuthorization(router: Pick<InjectedRouter, 'replace'>) {
  const returnTo = window.location.pathname + window.location.search + window.location.hash;
  router.replace({ pathname: '/sessions/new', query: { return_to: returnTo } });
  return requireAuthorizationAction();
}

export function registerBranchStatus(branchLike: BranchLike, component: string, status: T.Status) {
  return (dispatch: Dispatch) => {
    dispatch(registerBranchStatusAction(branchLike, component, status));
  };
}
