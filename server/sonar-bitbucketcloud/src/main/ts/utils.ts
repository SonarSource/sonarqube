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

import { parse } from 'querystring';
import { resetBundle } from '@sqcore/helpers/l10n';
import l10nbundle from './l10nbundle';
import { WidgetType, AppContext, PullRequestContext } from './types';

declare const AP: {
  getLocation: (callback: (location: string) => void) => void;
  require: Function;
};
const query = parse<{ [key: string]: string }>(window.location.search.replace('?', ''));

export function getAppContext(): AppContext {
  return { jwt: (window as any).bbcJWT || query.jwt || '' };
}

export function getAppKey(): string {
  return (window as any).bbcAppKey || '';
}

export function getDisabled(): boolean {
  return query.disabled === 'true';
}

export function getWidgetKey(): WidgetType {
  return (window as any).bbcWidgetKey;
}

export function getProjectKey(): string | undefined {
  return (window as any).projectKey;
}

export function getPullRequestContext(): PullRequestContext {
  return {
    prId: (window as any).prId || query.prId,
    repoUuid: getRepoUuid()
  };
}

export function getRepoUuid(): string {
  return query.repoUuid;
}

export function isRepoAdmin(): boolean {
  return query.isRepoAdmin === 'true';
}

export function installLanguageBundle() {
  resetBundle(l10nbundle);
}

export function getRepoSettingsUrl(): Promise<string> {
  return new Promise(resolve =>
    AP.getLocation(location => {
      const parser = document.createElement('a');
      parser.href = location;
      resolve(
        `${parser.origin}/{}/${getRepoUuid()}/admin/addon/admin/${getAppKey()}/repository-config`
      );
    })
  );
}

export function displayDialog(options: { key: string; size?: string }) {
  return getBBCModule('dialog').then(dialog => {
    dialog.create(options);
  });
}

export function displayMessage(
  level: 'error' | 'info' | 'success' | 'warning',
  title: string,
  msg: string,
  options = { closeable: true, fadeout: true, delay: 5000 }
) {
  getBBCModule('messages').then(
    messages => {
      if (messages[level]) {
        messages[level](title, msg, options);
      }
    },
    () => {}
  );
}

export function apiRequest(data: {
  cache: boolean;
  contentType?: string;
  data?: any;
  headers?: any;
  type: string;
  url: string;
}) {
  return getBBCModule('request').then(
    request =>
      new Promise((resolve, reject) =>
        request({
          error: (
            _error: any,
            response: { statusCode: number; body: { errors: Array<{ msg: string }> } }
          ) => {
            reject({ statusCode: response.statusCode, errors: response.body.errors });
          },
          success: resolve,
          ...data
        })
      )
  );
}

export function proxyRequest(data: {
  cache: boolean;
  contentType?: string;
  data?: any;
  headers?: any;
  type: string;
  url: string;
}) {
  return getBBCModule('proxyRequest').then(
    proxyRequest =>
      new Promise((resolve, reject) =>
        proxyRequest({
          error: (
            _error: any,
            response: { statusCode: number; body: { errors: Array<{ msg: string }> } }
          ) => {
            reject({ statusCode: response.statusCode, errors: response.body.errors });
          },
          success: resolve,
          ...data
        })
      )
  );
}

const bbcModule: { [key: string]: any } = {};

export function getBBCModule(key: string) {
  if (bbcModule[key]) {
    return Promise.resolve(bbcModule[key]);
  }
  if (Object.prototype.hasOwnProperty.call(window, 'AP')) {
    return new Promise(resolve =>
      AP.require(key, (moduleRequired: any) => {
        bbcModule[key] = moduleRequired;
        resolve(bbcModule[key]);
      })
    );
  }
  return Promise.reject('Bitbucket cloud AP not defined');
}
