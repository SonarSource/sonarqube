/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { throwGlobalError } from '../helpers/error';
import { getJSON, postJSON } from '../helpers/request';

export enum MessageTypes {
  GlobalNcd90 = 'global_ncd_90',
  ProjectNcd90 = 'project_ncd_90',
  BranchNcd90 = 'branch_ncd_90',
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
  return postJSON('api/dismiss_message/dismiss', data).catch(throwGlobalError);
}
