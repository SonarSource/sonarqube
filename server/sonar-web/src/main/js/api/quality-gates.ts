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
import { getJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import { Metric } from '../app/types';

export interface ConditionBase {
  error: string;
  metric: string;
  op?: string;
  period?: number;
  warning: string;
}

export interface Condition extends ConditionBase {
  id: number;
}

export interface QualityGate {
  actions?: {
    associateProjects: boolean;
    copy: boolean;
    delete: boolean;
    manageConditions: boolean;
    rename: boolean;
    setAsDefault: boolean;
  };
  conditions?: Condition[];
  id: number;
  isBuiltIn?: boolean;
  isDefault?: boolean;
  name: string;
}

export function fetchQualityGates(data: {
  organization?: string;
}): Promise<{
  actions: { create: boolean };
  qualitygates: QualityGate[];
}> {
  return getJSON('/api/qualitygates/list', data).catch(throwGlobalError);
}

export function fetchQualityGate(data: {
  id: number;
  organization?: string;
}): Promise<QualityGate> {
  return getJSON('/api/qualitygates/show', data).catch(throwGlobalError);
}

export function createQualityGate(data: {
  name: string;
  organization?: string;
}): Promise<QualityGate> {
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
}): Promise<QualityGate> {
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
  } & ConditionBase
): Promise<Condition> {
  return postJSON('/api/qualitygates/create_condition', data);
}

export function updateCondition(data: { organization?: string } & Condition): Promise<Condition> {
  return postJSON('/api/qualitygates/update_condition', data);
}

export function deleteCondition(data: { id: number; organization?: string }): Promise<void> {
  return post('/api/qualitygates/delete_condition', data);
}

export function getGateForProject(data: {
  organization?: string;
  project: string;
}): Promise<QualityGate | undefined> {
  return getJSON('/api/qualitygates/get_by_project', data).then(
    ({ qualityGate }) =>
      qualityGate && {
        ...qualityGate,
        isDefault: qualityGate.default
      },
    throwGlobalError
  );
}

export function associateGateWithProject(data: {
  gateId: number;
  organization?: string;
  projectKey: string;
}): Promise<void | Response> {
  return post('/api/qualitygates/select', data).catch(throwGlobalError);
}

export function dissociateGateWithProject(data: {
  gateId: number;
  organization?: string;
  projectKey: string;
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
  metrics: Metric[];
  projects: ApplicationProject[];
  status: string;
}

export function getApplicationQualityGate(data: {
  application: string;
  organization?: string;
}): Promise<ApplicationQualityGate> {
  return getJSON('/api/qualitygates/application_status', data).catch(throwGlobalError);
}
