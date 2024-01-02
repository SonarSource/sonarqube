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
import { NewCodeDefinition, NewCodeDefinitionType } from '../types/new-code-definition';

export const DEFAULT_NEW_CODE_DEFINITION_TYPE: NewCodeDefinitionType =
  NewCodeDefinitionType.PreviousVersion;
export const NUMBER_OF_DAYS_MIN_VALUE = 1;
export const NUMBER_OF_DAYS_MAX_VALUE = 90;
export const NUMBER_OF_DAYS_DEFAULT_VALUE = 30;

export function isNewCodeDefinitionCompliant(newCodePeriod: NewCodeDefinition) {
  switch (newCodePeriod.type) {
    case NewCodeDefinitionType.NumberOfDays:
      if (!newCodePeriod.value) {
        return false;
      }
      return (
        /^\d+$/.test(newCodePeriod.value) &&
        NUMBER_OF_DAYS_MIN_VALUE <= +newCodePeriod.value &&
        +newCodePeriod.value <= NUMBER_OF_DAYS_MAX_VALUE
      );
    case NewCodeDefinitionType.SpecificAnalysis:
      return false;
    default:
      return true;
  }
}

export function getNumberOfDaysDefaultValue(
  globalNewCodeDefinition?: NewCodeDefinition | null,
  inheritedNewCodeDefinition?: NewCodeDefinition | null,
) {
  if (
    inheritedNewCodeDefinition &&
    isNewCodeDefinitionCompliant(inheritedNewCodeDefinition) &&
    inheritedNewCodeDefinition.type === NewCodeDefinitionType.NumberOfDays &&
    inheritedNewCodeDefinition.value
  ) {
    return inheritedNewCodeDefinition.value;
  }

  if (
    globalNewCodeDefinition &&
    isNewCodeDefinitionCompliant(globalNewCodeDefinition) &&
    globalNewCodeDefinition.type === NewCodeDefinitionType.NumberOfDays &&
    globalNewCodeDefinition.value
  ) {
    return globalNewCodeDefinition.value;
  }

  return NUMBER_OF_DAYS_DEFAULT_VALUE.toString();
}
