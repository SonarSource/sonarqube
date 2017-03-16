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
import { request, checkStatus, parseJSON, getJSON, post, postJSON } from '../helpers/request';

export function getQualityProfiles(data) {
  const url = '/api/qualityprofiles/search';
  return getJSON(url, data).then(r => r.profiles);
}

export function createQualityProfile(data) {
  return request('/api/qualityprofiles/create')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function restoreQualityProfile(data) {
  return request('/api/qualityprofiles/restore')
    .setMethod('post')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function getProfileProjects(data) {
  const url = '/api/qualityprofiles/projects';
  return getJSON(url, data);
}

export function getProfileInheritance(profileKey) {
  const url = '/api/qualityprofiles/inheritance';
  const data = { profileKey };
  return getJSON(url, data);
}

export function setDefaultProfile(profileKey) {
  const url = '/api/qualityprofiles/set_default';
  const data = { profileKey };
  return post(url, data);
}

/**
 * Rename profile
 * @param {string} key
 * @param {string} name
 * @returns {Promise}
 */
export function renameProfile(key, name) {
  const url = '/api/qualityprofiles/rename';
  const data = { key, name };
  return post(url, data);
}

/**
 * Copy profile
 * @param {string} fromKey
 * @param {string} toName
 * @returns {Promise}
 */
export function copyProfile(fromKey, toName) {
  const url = '/api/qualityprofiles/copy';
  const data = { fromKey, toName };
  return postJSON(url, data);
}

/**
 * Delete profile
 * @param {string} profileKey
 * @returns {Promise}
 */
export function deleteProfile(profileKey) {
  const url = '/api/qualityprofiles/delete';
  const data = { profileKey };
  return post(url, data);
}

/**
 * Change profile parent
 * @param {string} profileKey
 * @param {string} parentKey
 * @returns {Promise}
 */
export function changeProfileParent(profileKey, parentKey) {
  const url = '/api/qualityprofiles/change_parent';
  const data = { profileKey, parentKey };
  return post(url, data);
}

/**
 * Get list of available importers
 * @returns {Promise}
 */
export function getImporters() {
  const url = '/api/qualityprofiles/importers';
  return getJSON(url).then(r => r.importers);
}

/**
 * Get list of available exporters
 * @returns {Promise}
 */
export function getExporters() {
  const url = '/api/qualityprofiles/exporters';
  return getJSON(url).then(r => r.exporters);
}

/**
 * Restore built-in profiles
 * @param {string} languageKey
 * @returns {Promise}
 */
export function restoreBuiltInProfiles(languageKey) {
  const url = '/api/qualityprofiles/restore_built_in';
  const data = { language: languageKey };
  return post(url, data);
}

/**
 * Get changelog of a quality profile
 * @param {Object} data API parameters
 * @returns {Promise}
 */
export function getProfileChangelog(data) {
  const url = '/api/qualityprofiles/changelog';
  return getJSON(url, data);
}

/**
 * Compare two profiles
 * @param {string} leftKey
 * @param {string} rightKey
 * @returns {Promise}
 */
export function compareProfiles(leftKey, rightKey) {
  const url = '/api/qualityprofiles/compare';
  const data = { leftKey, rightKey };
  return getJSON(url, data);
}

export function associateProject(profileKey, projectKey) {
  const url = '/api/qualityprofiles/add_project';
  const data = { profileKey, projectKey };
  return post(url, data);
}

export function dissociateProject(profileKey, projectKey) {
  const url = '/api/qualityprofiles/remove_project';
  const data = { profileKey, projectKey };
  return post(url, data);
}
