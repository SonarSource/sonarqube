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
// @flow
import { request, checkStatus, parseJSON, getJSON, post, postJSON } from '../helpers/request';

export function searchQualityProfiles(data: { organization?: string, projectKey?: string }) {
  const url = '/api/qualityprofiles/search';
  return getJSON(url, data).then(r => r.profiles);
}

export function getQualityProfiles(data: { compareToSonarWay?: boolean, profile: string }) {
  const url = '/api/qualityprofiles/show';
  return getJSON(url, data);
}

export function createQualityProfile(data: Object) {
  return request('/api/qualityprofiles/create')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function restoreQualityProfile(data: Object) {
  return request('/api/qualityprofiles/restore')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function getProfileProjects(data: Object) {
  const url = '/api/qualityprofiles/projects';
  return getJSON(url, data);
}

export function getProfileInheritance(profileKey: string) {
  const url = '/api/qualityprofiles/inheritance';
  const data = { profileKey };
  return getJSON(url, data);
}

export function setDefaultProfile(profileKey: string) {
  const url = '/api/qualityprofiles/set_default';
  const data = { profileKey };
  return post(url, data);
}

export function renameProfile(key: string, name: string) {
  const url = '/api/qualityprofiles/rename';
  const data = { key, name };
  return post(url, data);
}

export function copyProfile(fromKey: string, toName: string) {
  const url = '/api/qualityprofiles/copy';
  const data = { fromKey, toName };
  return postJSON(url, data);
}

export function deleteProfile(profileKey: string) {
  const url = '/api/qualityprofiles/delete';
  const data = { profileKey };
  return post(url, data);
}

export function changeProfileParent(profileKey: string, parentKey: string) {
  const url = '/api/qualityprofiles/change_parent';
  const data = { profileKey, parentKey };
  return post(url, data);
}

export function getImporters() {
  const url = '/api/qualityprofiles/importers';
  return getJSON(url).then(r => r.importers);
}

export function getExporters() {
  const url = '/api/qualityprofiles/exporters';
  return getJSON(url).then(r => r.exporters);
}

export function getProfileChangelog(data: Object) {
  const url = '/api/qualityprofiles/changelog';
  return getJSON(url, data);
}

export function compareProfiles(leftKey: string, rightKey: string) {
  const url = '/api/qualityprofiles/compare';
  const data = { leftKey, rightKey };
  return getJSON(url, data);
}

export function associateProject(profileKey: string, projectKey: string) {
  const url = '/api/qualityprofiles/add_project';
  const data = { profileKey, projectKey };
  return post(url, data);
}

export function dissociateProject(profileKey: string, projectKey: string) {
  const url = '/api/qualityprofiles/remove_project';
  const data = { profileKey, projectKey };
  return post(url, data);
}
