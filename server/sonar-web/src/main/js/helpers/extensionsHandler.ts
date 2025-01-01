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

// Do not import dependencies in this helper, to keep initial bundle load as small as possible

import { ExtensionRegistryEntry, ExtensionStartMethod } from '../types/extension';
import { Dict } from '../types/types';
import { getEnhancedWindow } from './browser';

const WEB_ANALYTICS_EXTENSION = 'sq-web-analytics';

const extensions: Dict<ExtensionRegistryEntry> = {};

function registerExtension(key: string, start: ExtensionStartMethod, providesCSSFile = false) {
  extensions[key] = { start, providesCSSFile };
}

function setWebAnalyticsPageChangeHandler(pageHandler: (pathname: string) => void) {
  registerExtension(WEB_ANALYTICS_EXTENSION, pageHandler);
}

export function installExtensionsHandler() {
  getEnhancedWindow().registerExtension = registerExtension;
}

export function installWebAnalyticsHandler() {
  getEnhancedWindow().setWebAnalyticsPageChangeHandler = setWebAnalyticsPageChangeHandler;
}

export function getExtensionFromCache(key: string): ExtensionRegistryEntry | undefined {
  return extensions[key];
}

export function getWebAnalyticsPageHandlerFromCache(): Function | undefined {
  return extensions[WEB_ANALYTICS_EXTENSION]?.start;
}
