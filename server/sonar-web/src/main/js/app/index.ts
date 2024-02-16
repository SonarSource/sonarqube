/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

/* NOTE: esbuild will transpile the _syntax_ down to what the TARGET_BROWSERS (in config/utils) */
/* understand. It will _not_, however, polyfill missing API methods, such as                    */
/* String.prototype.replaceAll. This is why we also import core-js.                             */
import 'core-js/stable';
/*                                                                                              */
import axios from 'axios';
import { addGlobalErrorMessage } from 'design-system';
import 'react-day-picker/dist/style.css';
import { getAvailableFeatures } from '../api/features';
import { getGlobalNavigation } from '../api/navigation';
import { getCurrentUser } from '../api/users';
import { installExtensionsHandler, installWebAnalyticsHandler } from '../helpers/extensionsHandler';
import { loadL10nBundle } from '../helpers/l10nBundle';
import { axiosToCatch, parseErrorResponse } from '../helpers/request';
import { getBaseUrl, getSystemStatus, initAppVariables } from '../helpers/system';
import './styles/sonar.ts';

installWebAnalyticsHandler();
installExtensionsHandler();
initAppVariables();
initApplication();

async function initApplication() {
  axiosToCatch.interceptors.response.use((response) => response.data);
  axiosToCatch.defaults.baseURL = getBaseUrl();
  axiosToCatch.defaults.headers.patch['Content-Type'] = 'application/merge-patch+json';
  axios.defaults.headers.patch['Content-Type'] = 'application/merge-patch+json';
  axios.defaults.baseURL = getBaseUrl();

  axios.interceptors.response.use(
    (response) => response.data,
    (error) => {
      const { response } = error;
      addGlobalErrorMessage(parseErrorResponse(response));

      return Promise.reject(response);
    },
  );

  const [l10nBundle, currentUser, appState, availableFeatures] = await Promise.all([
    loadL10nBundle(),
    isMainApp() ? getCurrentUser() : undefined,
    isMainApp() ? getGlobalNavigation() : undefined,
    isMainApp() ? getAvailableFeatures() : undefined,
  ]).catch((error) => {
    // eslint-disable-next-line no-console
    console.error('Application failed to start', error);
    throw error;
  });

  const startReactApp = await import('./utils/startReactApp').then((i) => i.default);
  startReactApp(l10nBundle, currentUser, appState, availableFeatures);
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
