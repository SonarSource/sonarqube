/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import throwGlobalError from '../app/utils/throwGlobalError';
import { getJSON, post, postJSON } from '../sonar-ui-common/helpers/request';
import { BranchParameters } from '../types/branch-like';
import { QualityGateApplicationStatus, QualityGateProjectStatus } from '../types/quality-gates';

export function fetchQualityGates(): Promise<{
  actions: { create: boolean };
  qualitygates: T.QualityGate[];
}> {
  return getJSON('/api/qualitygates/list').catch(throwGlobalError);
}

export function fetchQualityGate(data: { id: number | string }): Promise<T.QualityGate> {
  return getJSON('/api/qualitygates/show', data).catch(throwGlobalError);
}

export function createQualityGate(data: { name: string }): Promise<T.QualityGate> {
  return postJSON('/api/qualitygates/create', data).catch(throwGlobalError);
}

export function deleteQualityGate(data: { id: string }): Promise<void | Response> {
  return post('/api/qualitygates/destroy', data).catch(throwGlobalError);
}

export function renameQualityGate(data: { id: string; name: string }): Promise<void | Response> {
  return post('/api/qualitygates/rename', data).catch(throwGlobalError);
}

export function copyQualityGate(data: { id: string; name: string }): Promise<T.QualityGate> {
  return postJSON('/api/qualitygates/copy', data).catch(throwGlobalError);
}

export function setQualityGateAsDefault(data: { id: string }): Promise<void | Response> {
  return post('/api/qualitygates/set_as_default', data).catch(throwGlobalError);
}

export function createCondition(
  data: {
    gateId: string;
  } & T.Omit<T.Condition, 'id'>
): Promise<T.Condition> {
  return postJSON('/api/qualitygates/create_condition', data).catch(throwGlobalError);
}

export function updateCondition(data: T.Condition): Promise<T.Condition> {
  return postJSON('/api/qualitygates/update_condition', data).catch(throwGlobalError);
}

export function deleteCondition(data: { id: number }): Promise<void> {
  return post('/api/qualitygates/delete_condition', data);
}

export function getGateForProject(data: { project: string }): Promise<T.QualityGate | undefined> {
  return getJSON('/api/qualitygates/get_by_project', data).then(
    ({ qualityGate }) =>
      qualityGate && {
        ...qualityGate,
        isDefault: qualityGate.default
      },
    throwGlobalError
  );
}

export function searchProjects(data: {
  gateName: string;
  page?: number;
  pageSize?: number;
  query?: string;
  selected?: string;
}): Promise<{
  paging: T.Paging;
  results: Array<{ key: string; name: string; selected: boolean }>;
}> {
  return getJSON('/api/qualitygates/search', data).catch(throwGlobalError);
}

export function associateGateWithProject(data: {
  gateId: string;
  projectKey: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/select', data).catch(throwGlobalError);
}

export function dissociateGateWithProject(data: {
  gateId: string;
  projectKey: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/deselect', data).catch(throwGlobalError);
}

export function getApplicationQualityGate(data: {
  application: string;
  branch?: string;
}): Promise<QualityGateApplicationStatus> {
  return getJSON('/api/qualitygates/application_status', data).catch(throwGlobalError);
}

export function getQualityGateProjectStatus(
  data: {
    projectKey?: string;
    projectId?: string;
  } & BranchParameters
): Promise<QualityGateProjectStatus> {
  return getJSON('/api/qualitygates/project_status', data)
    .then(r => r.projectStatus)
    .catch(throwGlobalError);
}
