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

import { hasGlobalPermission } from '../../helpers/users';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../types/new-code-definition';
import { Permissions } from '../../types/permissions';
import { Component } from '../../types/types';
import { CurrentUser, isLoggedIn } from '../../types/users';

export type PreviouslyNonCompliantNCD = NewCodeDefinition &
  Required<Pick<NewCodeDefinition, 'previousNonCompliantValue' | 'updatedAt'>>;

export function isPreviouslyNonCompliantDaysNCD(
  newCodeDefinition: NewCodeDefinition
): newCodeDefinition is PreviouslyNonCompliantNCD {
  return (
    newCodeDefinition.type === NewCodeDefinitionType.NumberOfDays &&
    newCodeDefinition.previousNonCompliantValue !== undefined &&
    newCodeDefinition.updatedAt !== undefined
  );
}

export function isGlobalOrProjectAdmin(currentUser: CurrentUser, component?: Component) {
  if (!isLoggedIn(currentUser)) {
    return false;
  }

  if (component !== undefined) {
    return !!component.configuration?.showSettings;
  }

  return hasGlobalPermission(currentUser, Permissions.Admin);
}
