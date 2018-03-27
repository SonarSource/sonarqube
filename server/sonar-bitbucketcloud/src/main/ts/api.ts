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
import { parseError, parseJSON, request, RequestData } from '@sqcore/helpers/request';
import { Paging, MyProject, IdentityProvider } from '@sqcore/app/types';
import {
  AppContext,
  ProjectData,
  RepositoryData,
  RepositoryMeasure,
  PullRequestData,
  PullRequestContext
} from './types';
import { displayMessage, proxyRequest, apiRequest, getAppKey, getRepoUuid } from './utils';

export function bindProject(data: AppContext & { projectKey: string }): Promise<void | Response> {
  return post('/integration/bitbucketcloud/bind_repo', data).catch(displayWSError);
}

export function displayWSError({ response }: { response: Response }): Promise<Response> {
  parseError({ response }).then(
    msg => {
      displayMessage('error', 'SonarCloud Error', msg);
    },
    () => {}
  );
  return Promise.reject(response);
}

export function getIdentityProviders(): Promise<{ identityProviders: IdentityProvider[] }> {
  return getJSON('/api/users/identity_providers');
}

export function getMyProjects(data: {
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; projects: MyProject[] }> {
  return getJSON('/api/projects/search_my_projects', data);
}

export function getRepositoryData(data: AppContext): Promise<ProjectData> {
  return getJSON('/integration/bitbucketcloud/repo_widget_data', data).then(
    (repoData: RepositoryData) => {
      const measures: { [key: string]: string } = {};
      repoData.measures.forEach((measure: RepositoryMeasure) => {
        if (measure.value !== undefined) {
          measures[measure.metric] = measure.value;
        }
      });
      return { ...repoData, measures };
    }
  );
}

export function getStoredProperty(property: string) {
  return apiRequest({
    cache: false,
    type: 'GET',
    url: `/2.0/repositories/{}/${getRepoUuid()}/properties/${getAppKey()}/${property}`
  });
}

export function putStoredProperty(property: string, value: Object | string | boolean | number) {
  return apiRequest({
    cache: false,
    contentType: 'application/json',
    data: value,
    type: 'PUT',
    url: `/2.0/repositories/{}/${getRepoUuid()}/properties/${getAppKey()}/${property}`
  });
}

export function getPullRequestData(context: PullRequestContext): Promise<PullRequestData> {
  return getProxyJSON(
    `/repositories/${context.repoUuid}/pullrequests/${context.prId}/code_quality`
  );
}

function checkStatus(response: Response): Promise<Response> {
  return new Promise((resolve, reject) => {
    if (response.status >= 200 && response.status < 300) {
      resolve(response);
    } else {
      reject({ response });
    }
  });
}

function getProxyJSON(url: string): Promise<any> {
  return proxyRequest({ cache: false, type: 'GET', url });
}

function getJSON(url: string, data?: RequestData): Promise<any> {
  return request(url)
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

function post(url: string, data?: RequestData): Promise<void> {
  return new Promise((resolve, reject) => {
    request(url)
      .setMethod('POST')
      .setData(data)
      .submit()
      .then(checkStatus)
      .then(() => resolve(), reject);
  });
}
