/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { installExtensionsHandler, installWebAnalyticsHandler } from '../helpers/extensionsHandler';
import { loadL10nBundle } from '../helpers/l10n';
import { parseJSON, request } from '../helpers/request';
import { getBaseUrl, getSystemStatus } from '../helpers/system';
import { AppState } from '../types/types';
import './styles/sonar.ts';

installWebAnalyticsHandler();

if (isMainApp()) {
  installExtensionsHandler();

  Promise.all([loadL10nBundle(), loadUser(), loadAppState(), loadApp()]).then(
    ([l10nBundle, user, appState, startReactApp]) => {
      startReactApp(l10nBundle.locale, appState, user);
    },
    error => {
      if (isResponse(error) && error.status === 401) {
        redirectToLogin();
      } else {
        logError(error);
      }
    }
  );
} else {
  // login, maintenance or setup pages

  const appStatePromise: Promise<AppState> = new Promise(resolve => {
    loadAppState()
      .then(data => {
        resolve(data);
      })
      .catch(() => {
        resolve({
          edition: undefined,
          productionDatabase: true,
          qualifiers: [],
          settings: {},
          version: ''
        });
      });
  });

  Promise.all([loadL10nBundle(), appStatePromise, loadApp()]).then(
    ([l10nBundle, appState, startReactApp]) => {
      startReactApp(l10nBundle.locale, appState);
    },
    error => {
      logError(error);
    }
  );
}

function loadUser() {
  return request('/api/users/current')
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

function loadAppState() {
  return request('/api/navigation/global')
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

function loadApp() {
  return import(/* webpackChunkName: 'app' */ './utils/startReactApp').then(i => i.default);
}

function checkStatus(response: Response) {
  return new Promise((resolve, reject) => {
    if (response.status >= 200 && response.status < 300) {
      resolve(response);
    } else {
      reject(response);
    }
  });
}

function isResponse(error: any): error is Response {
  return typeof error.status === 'number';
}

function redirectToLogin() {
  const returnTo = window.location.pathname + window.location.search + window.location.hash;
  window.location.href = `${getBaseUrl()}/sessions/new?return_to=${encodeURIComponent(returnTo)}`;
}

function logError(error: any) {
  // eslint-disable-next-line no-console
  console.error('Application failed to start!', error);
}

function isMainApp() {
  const { pathname } = window.location;
  return (
    getSystemStatus() === 'UP' &&
    !pathname.startsWith(`${getBaseUrl()}/sessions`) &&
    !pathname.startsWith(`${getBaseUrl()}/maintenance`) &&
    !pathname.startsWith(`${getBaseUrl()}/setup`) &&
    !pathname.startsWith(`${getBaseUrl()}/formatting/help`)
  );
}
