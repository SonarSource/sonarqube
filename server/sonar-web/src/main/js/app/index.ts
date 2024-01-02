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
import { getAvailableFeatures } from '../api/features';
import { getGlobalNavigation } from '../api/navigation';
import { getCurrentUser } from '../api/users';
import { installExtensionsHandler, installWebAnalyticsHandler } from '../helpers/extensionsHandler';
import { loadL10nBundle } from '../helpers/l10nBundle';
import { getBaseUrl, getSystemStatus } from '../helpers/system';
import './styles/sonar.ts';

installWebAnalyticsHandler();
installExtensionsHandler();
initApplication();

async function initApplication() {
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
  startReactApp(l10nBundle.locale, currentUser, appState, availableFeatures);
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
