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
import { getJSON, post, postJSON } from '../helpers/request';

export function fetchQualityGatesAppDetails() {
  const url = '/api/qualitygates/app';

  return getJSON(url);
}

export function fetchQualityGates() {
  const url = '/api/qualitygates/list';

  return getJSON(url).then(r =>
    r.qualitygates.map(qualityGate => {
      return {
        ...qualityGate,
        isDefault: qualityGate.id === r.default
      };
    })
  );
}

export function fetchQualityGate(id) {
  const url = '/api/qualitygates/show';
  return getJSON(url, { id });
}

export function createQualityGate(name) {
  const url = '/api/qualitygates/create';
  return postJSON(url, { name });
}

export function deleteQualityGate(id) {
  const url = '/api/qualitygates/destroy';
  return post(url, { id });
}

export function renameQualityGate(id, name) {
  const url = '/api/qualitygates/rename';
  return post(url, { id, name });
}

export function copyQualityGate(id, name) {
  const url = '/api/qualitygates/copy';
  return postJSON(url, { id, name });
}

export function setQualityGateAsDefault(id) {
  const url = '/api/qualitygates/set_as_default';
  return post(url, { id });
}

export function unsetQualityGateAsDefault(id) {
  const url = '/api/qualitygates/unset_default';
  return post(url, { id });
}

export function createCondition(gateId, condition) {
  const url = '/api/qualitygates/create_condition';
  return postJSON(url, { ...condition, gateId });
}

export function updateCondition(condition) {
  const url = '/api/qualitygates/update_condition';
  return postJSON(url, { ...condition });
}

export function deleteCondition(id) {
  const url = '/api/qualitygates/delete_condition';
  return post(url, { id });
}

export function getGateForProject(projectKey) {
  const url = '/api/qualitygates/get_by_project';
  const data = { projectKey };
  return getJSON(url, data).then(r => r.qualityGate);
}

export function associateGateWithProject(gateId, projectKey) {
  const url = '/api/qualitygates/select';
  const data = { gateId, projectKey };
  return post(url, data);
}

export function dissociateGateWithProject(gateId, projectKey) {
  const url = '/api/qualitygates/deselect';
  const data = { gateId, projectKey };
  return post(url, data);
}

export function getApplicationQualityGate(application) {
  return getJSON('/api/qualitygates/application_status', { application });
}
