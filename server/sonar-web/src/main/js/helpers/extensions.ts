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
import { getBaseUrl } from './urls';
import exposeLibraries from '../app/components/extensions/exposeLibraries';

const WEB_ANALYTICS_EXTENSION = 'sq-web-analytics';
const extensions: T.Dict<Function> = {};

function registerExtension(key: string, start: Function) {
  extensions[key] = start;
}

function setWebAnalyticsPageChangeHandler(pageHandler: (pathname: string) => void) {
  registerExtension(WEB_ANALYTICS_EXTENSION, pageHandler);
}

export function installExtensionsHandler() {
  (window as any).registerExtension = registerExtension;
}

export function installWebAnalyticsHandler() {
  (window as any).setWebAnalyticsPageChangeHandler = setWebAnalyticsPageChangeHandler;
}

export function getExtensionFromCache(key: string): Function | undefined {
  return extensions[key];
}

export function getWebAnalyticsPageHandlerFromCache(): Function | undefined {
  return extensions[WEB_ANALYTICS_EXTENSION];
}

export function installScript(url: string, target: 'body' | 'head' = 'body'): Promise<any> {
  return new Promise(resolve => {
    const scriptTag = document.createElement('script');
    scriptTag.src = `${getBaseUrl()}${url}`;
    scriptTag.onload = resolve;
    document.getElementsByTagName(target)[0].appendChild(scriptTag);
  });
}

export async function getExtensionStart(key: string) {
  const fromCache = getExtensionFromCache(key);
  if (fromCache) {
    return Promise.resolve(fromCache);
  }

  exposeLibraries();
  await installScript(`/static/${key}.js`);

  const start = getExtensionFromCache(key);
  if (start) {
    return start;
  }
  return Promise.reject();
}
