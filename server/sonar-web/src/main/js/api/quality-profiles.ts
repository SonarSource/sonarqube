/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import {
  request,
  checkStatus,
  parseJSON,
  getJSON,
  post,
  postJSON,
  RequestData
} from '../helpers/request';

export function searchQualityProfiles(data: {
  organization?: string;
  projectKey?: string;
}): Promise<any> {
  return getJSON('/api/qualityprofiles/search', data).then(r => r.profiles);
}

export function getQualityProfiles(data: {
  compareToSonarWay?: boolean;
  profile: string;
}): Promise<any> {
  return getJSON('/api/qualityprofiles/show', data);
}

export function createQualityProfile(data: RequestData): Promise<any> {
  return request('/api/qualityprofiles/create')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function restoreQualityProfile(data: RequestData): Promise<any> {
  return request('/api/qualityprofiles/restore')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function getProfileProjects(data: RequestData): Promise<any> {
  return getJSON('/api/qualityprofiles/projects', data);
}

export function getProfileInheritance(profileKey: string): Promise<any> {
  return getJSON('/api/qualityprofiles/inheritance', { profileKey });
}

export function setDefaultProfile(profileKey: string): Promise<void> {
  return post('/api/qualityprofiles/set_default', { profileKey });
}

export function renameProfile(key: string, name: string): Promise<void> {
  return post('/api/qualityprofiles/rename', { key, name });
}

export function copyProfile(fromKey: string, toName: string): Promise<any> {
  return postJSON('/api/qualityprofiles/copy', { fromKey, toName });
}

export function deleteProfile(profileKey: string): Promise<void> {
  return post('/api/qualityprofiles/delete', { profileKey });
}

export function changeProfileParent(profileKey: string, parentKey: string): Promise<void> {
  return post('/api/qualityprofiles/change_parent', { profileKey, parentKey });
}

export function getImporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/importers').then(r => r.importers);
}

export function getExporters(): Promise<any> {
  return getJSON('/api/qualityprofiles/exporters').then(r => r.exporters);
}

export function getProfileChangelog(data: RequestData): Promise<any> {
  return getJSON('/api/qualityprofiles/changelog', data);
}

export function compareProfiles(leftKey: string, rightKey: string): Promise<any> {
  return getJSON('/api/qualityprofiles/compare', { leftKey, rightKey });
}

export function associateProject(profileKey: string, projectKey: string): Promise<void> {
  return post('/api/qualityprofiles/add_project', { profileKey, projectKey });
}

export function dissociateProject(profileKey: string, projectKey: string): Promise<void> {
  return post('/api/qualityprofiles/remove_project', { profileKey, projectKey });
}
