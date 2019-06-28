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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function fetchQualityGates(data: {
  organization?: string;
}): Promise<{
  actions: { create: boolean };
  qualitygates: T.QualityGate[];
}> {
  return getJSON('/api/qualitygates/list', data).catch(throwGlobalError);
}

export function fetchQualityGate(data: {
  id: number | string;
  organization?: string;
}): Promise<T.QualityGate> {
  return getJSON('/api/qualitygates/show', data).catch(throwGlobalError);
}

export function createQualityGate(data: {
  name: string;
  organization?: string;
}): Promise<T.QualityGate> {
  return postJSON('/api/qualitygates/create', data).catch(throwGlobalError);
}

export function deleteQualityGate(data: {
  id: number;
  organization?: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/destroy', data).catch(throwGlobalError);
}

export function renameQualityGate(data: {
  id: number;
  name: string;
  organization?: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/rename', data).catch(throwGlobalError);
}

export function copyQualityGate(data: {
  id: number;
  name: string;
  organization?: string;
}): Promise<T.QualityGate> {
  return postJSON('/api/qualitygates/copy', data).catch(throwGlobalError);
}

export function setQualityGateAsDefault(data: {
  id: number;
  organization?: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/set_as_default', data).catch(throwGlobalError);
}

export function createCondition(
  data: {
    gateId: number;
    organization?: string;
  } & T.Omit<T.Condition, 'id'>
): Promise<T.Condition> {
  return postJSON('/api/qualitygates/create_condition', data).catch(throwGlobalError);
}

export function updateCondition(
  data: { organization?: string } & T.Condition
): Promise<T.Condition> {
  return postJSON('/api/qualitygates/update_condition', data).catch(throwGlobalError);
}

export function deleteCondition(data: { id: number; organization?: string }): Promise<void> {
  return post('/api/qualitygates/delete_condition', data);
}

export function getGateForProject(data: {
  organization?: string;
  project: string;
}): Promise<T.QualityGate | undefined> {
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
  gateId: number;
  organization?: string;
  page?: number;
  pageSize?: number;
  query?: string;
  selected?: string;
}): Promise<{
  paging: T.Paging;
  results: Array<{ id: string; key: string; name: string; selected: boolean }>;
}> {
  return getJSON('/api/qualitygates/search', data).catch(throwGlobalError);
}

export function associateGateWithProject(data: {
  gateId: number;
  organization?: string;
  projectKey?: string;
  projectId?: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/select', data).catch(throwGlobalError);
}

export function dissociateGateWithProject(data: {
  gateId: number;
  organization?: string;
  projectKey?: string;
  projectId?: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/deselect', data).catch(throwGlobalError);
}

export interface ConditionAnalysis {
  comparator: string;
  errorThreshold?: string;
  metric: string;
  periodIndex?: number;
  onLeak?: boolean;
  status: string;
  value: string;
  warningThreshold?: string;
}

export interface ApplicationProject {
  key: string;
  name: string;
  status: string;
  conditions: ConditionAnalysis[];
}

export interface ApplicationQualityGate {
  metrics: T.Metric[];
  projects: ApplicationProject[];
  status: string;
}

export function getApplicationQualityGate(data: {
  application: string;
  branch?: string;
  organization?: string;
}): Promise<ApplicationQualityGate> {
  return getJSON('/api/qualitygates/application_status', data).catch(throwGlobalError);
}

export function getQualityGateProjectStatus(
  data: {
    projectKey?: string;
    projectId?: string;
  } & T.BranchParameters
): Promise<T.QualityGateProjectStatus> {
  return getJSON('/api/qualitygates/project_status', data)
    .then(r => r.projectStatus)
    .catch(throwGlobalError);
}
