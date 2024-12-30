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

import { getBaseUrl } from '../helpers/system';
import { ExtensionStartMethod } from '../types/extension';
import { getExtensionFromCache } from './extensionsHandler';

let librariesExposed = false;

export function installStyles(url: string, target: 'body' | 'head' = 'head'): Promise<any> {
  return new Promise((resolve) => {
    const linkTag = document.createElement('link');
    linkTag.href = `${getBaseUrl()}${url}`;
    linkTag.rel = 'stylesheet';
    linkTag.onload = resolve;
    document.getElementsByTagName(target)[0].appendChild(linkTag);
  });
}

export function installScript(url: string, target: 'body' | 'head' = 'body'): Promise<any> {
  return new Promise((resolve) => {
    const scriptTag = document.createElement('script');
    scriptTag.src = `${getBaseUrl()}${url}`;
    scriptTag.onload = resolve;
    document.getElementsByTagName(target)[0].appendChild(scriptTag);
  });
}

export async function getExtensionStart(key: string): Promise<ExtensionStartMethod | undefined> {
  const fromCache = getExtensionFromCache(key);
  if (fromCache) {
    return Promise.resolve(fromCache.start);
  }

  if (!librariesExposed) {
    librariesExposed = true;
    // Async import allows to reduce initial vendor bundle size
    const exposeLibraries = (await import('../app/components/extensions/exposeLibraries')).default;
    exposeLibraries();
  }

  await installScript(`/static/${key}.js`);

  const extension = getExtensionFromCache(key);
  if (!extension) {
    return Promise.reject();
  }

  if (extension.providesCSSFile) {
    await installStyles(`/static/${key}.css`);
  }

  return extension.start;
}
