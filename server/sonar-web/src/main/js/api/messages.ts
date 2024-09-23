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

export enum MessageTypes {
  GlobalNcd90 = 'GLOBAL_NCD_90',
  GlobalNcdPage90 = 'GLOBAL_NCD_PAGE_90',
  ProjectNcd90 = 'PROJECT_NCD_90',
  ProjectNcdPage90 = 'PROJECT_NCD_PAGE_90',
  BranchNcd90 = 'BRANCH_NCD_90',
  UnresolvedFindingsInAIGeneratedCode = 'UNRESOLVED_FINDINGS_IN_AI_GENERATED_CODE',
}

export interface MessageDismissParams {
  messageType: MessageTypes;
  projectKey?: string;
}

export function checkMessageDismissed(data: MessageDismissParams): Promise<{
  dismissed: boolean;
}> {
  return getJSON('/api/dismiss_message/check', data).catch(throwGlobalError);
}

export function setMessageDismissed(data: MessageDismissParams): Promise<void> {
  return post('/api/dismiss_message/dismiss', data).catch(throwGlobalError);
}
