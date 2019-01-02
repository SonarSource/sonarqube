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
import exposeLibraries from './exposeLibraries';
import { getExtensionFromCache } from '../../utils/installExtensionsHandler';
import { getBaseUrl } from '../../../helpers/urls';

function installScript(key: string) {
  return new Promise(resolve => {
    exposeLibraries();
    const scriptTag = document.createElement('script');
    scriptTag.src = `${getBaseUrl()}/static/${key}.js`;
    scriptTag.onload = resolve;
    document.getElementsByTagName('body')[0].appendChild(scriptTag);
  });
}

export function getExtensionStart(key: string): Promise<Function> {
  return new Promise((resolve, reject) => {
    const fromCache = getExtensionFromCache(key);
    if (fromCache) {
      resolve(fromCache);
      return;
    }

    installScript(key).then(() => {
      const start = getExtensionFromCache(key);
      if (start) {
        resolve(start);
      } else {
        reject();
      }
    }, reject);
  });
}
