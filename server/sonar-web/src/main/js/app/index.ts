/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { DEFAULT_LANGUAGE, installGlobal, requestMessages } from 'sonar-ui-common/helpers/l10n';
import { parseJSON, request } from 'sonar-ui-common/helpers/request';
import { installExtensionsHandler, installWebAnalyticsHandler } from '../helpers/extensionsHandler';
import { getSystemStatus } from '../helpers/system';
import './styles/sonar.css';

installGlobal();
installWebAnalyticsHandler();

if (isMainApp()) {
  installExtensionsHandler();

  Promise.all([loadMessages(), loadUser(), loadAppState(), loadApp()]).then(
    ([lang, user, appState, startReactApp]) => {
      startReactApp(lang, user, appState);
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

  const appStatePromise: Promise<T.AppState> = new Promise(resolve =>
    loadAppState()
      .then(data => resolve(data))
      .catch(() => resolve(undefined))
  );

  Promise.all([loadMessages(), appStatePromise, loadApp()]).then(
    ([lang, appState, startReactApp]) => {
      startReactApp(lang, undefined, appState);
    },
    error => {
      logError(error);
    }
  );
}

function loadMessages() {
  return requestMessages().then(setLanguage, setLanguage);
}

function loadLocaleData(langToLoad: string) {
  return Promise.all([import('react-intl/locale-data/' + langToLoad), import('react-intl')]).then(
    ([intlBundle, intl]) => {
      intl.addLocaleData(intlBundle.default);
    }
  );
}

function setLanguage(lang: string) {
  const langToLoad = lang || DEFAULT_LANGUAGE;
  // No need to load english (default) bundle, it's coming with react-intl
  if (langToLoad !== DEFAULT_LANGUAGE) {
    return loadLocaleData(langToLoad).then(() => langToLoad, () => DEFAULT_LANGUAGE);
  } else {
    return DEFAULT_LANGUAGE;
  }
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
    !pathname.startsWith(`${getBaseUrl()}/markdown/help`)
  );
}

function getBaseUrl(): string {
  return (window as any).baseUrl;
}
