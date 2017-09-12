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
import { getJSON, post, postJSON, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function fetchQualityGatesAppDetails(): Promise<any> {
  return getJSON('/api/qualitygates/app').catch(throwGlobalError);
}

export interface QualityGate {
  isDefault?: boolean;
  id: string;
  name: string;
}

export function fetchQualityGates(): Promise<QualityGate[]> {
  return getJSON('/api/qualitygates/list').then(
    r =>
      r.qualitygates.map((qualityGate: any) => {
        return {
          ...qualityGate,
          id: String(qualityGate.id),
          isDefault: qualityGate.id === r.default
        };
      }),
    throwGlobalError
  );
}

export function fetchQualityGate(id: string): Promise<any> {
  return getJSON('/api/qualitygates/show', { id }).catch(throwGlobalError);
}

export function createQualityGate(name: string): Promise<any> {
  return postJSON('/api/qualitygates/create', { name });
}

export function deleteQualityGate(id: string): Promise<void> {
  return post('/api/qualitygates/destroy', { id });
}

export function renameQualityGate(id: string, name: string): Promise<void> {
  return post('/api/qualitygates/rename', { id, name });
}

export function copyQualityGate(id: string, name: string): Promise<any> {
  return postJSON('/api/qualitygates/copy', { id, name });
}

export function setQualityGateAsDefault(id: string): Promise<void | Response> {
  return post('/api/qualitygates/set_as_default', { id }).catch(throwGlobalError);
}

export function unsetQualityGateAsDefault(id: string): Promise<void | Response> {
  return post('/api/qualitygates/unset_default', { id }).catch(throwGlobalError);
}

export function createCondition(gateId: string, condition: RequestData): Promise<any> {
  return postJSON('/api/qualitygates/create_condition', { ...condition, gateId });
}

export function updateCondition(condition: RequestData): Promise<any> {
  return postJSON('/api/qualitygates/update_condition', condition);
}

export function deleteCondition(id: string): Promise<void> {
  return post('/api/qualitygates/delete_condition', { id });
}

export function getGateForProject(projectKey: string): Promise<QualityGate | undefined> {
  return getJSON('/api/qualitygates/get_by_project', { projectKey }).then(
    r =>
      r.qualityGate && {
        id: r.qualityGate.id,
        isDefault: r.qualityGate.default,
        name: r.qualityGate.name
      }
  );
}

export function associateGateWithProject(
  gateId: string,
  projectKey: string
): Promise<void | Response> {
  return post('/api/qualitygates/select', { gateId, projectKey }).catch(throwGlobalError);
}

export function dissociateGateWithProject(
  gateId: string,
  projectKey: string
): Promise<void | Response> {
  return post('/api/qualitygates/deselect', { gateId, projectKey }).catch(throwGlobalError);
}

export function getApplicationQualityGate(application: string): Promise<any> {
  return getJSON('/api/qualitygates/application_status', { application });
}
