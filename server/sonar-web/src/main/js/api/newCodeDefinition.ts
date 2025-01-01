/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { post } from '../helpers/request';
import {
  NewCodeDefinition,
  NewCodeDefinitionBranch,
  NewCodeDefinitionType,
} from '../types/new-code-definition';

export function getNewCodeDefinition(data?: {
  branch?: string;
  project?: string;
}): Promise<Omit<NewCodeDefinition, 'effectiveValue'>> {
  return getJSON('/api/new_code_periods/show', data).catch(throwGlobalError);
}

export function setNewCodeDefinition(data: {
  branch?: string;
  project?: string;
  type: NewCodeDefinitionType;
  value?: string;
}): Promise<void> {
  return post('/api/new_code_periods/set', data).catch(throwGlobalError);
}

export function resetNewCodeDefinition(data: { branch?: string; project?: string }): Promise<void> {
  return post('/api/new_code_periods/unset', data).catch(throwGlobalError);
}

export function listBranchesNewCodeDefinition(data: {
  project: string;
}): Promise<{ newCodePeriods: NewCodeDefinitionBranch[] }> {
  return getJSON('/api/new_code_periods/list', data).catch(throwGlobalError);
}
