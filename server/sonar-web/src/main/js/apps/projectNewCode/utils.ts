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

import { isNewCodeDefinitionCompliant } from '../../helpers/new-code-definition';
import { NewCodeDefinitionType } from '../../types/new-code-definition';

export function getSettingValue({
  analysis,
  numberOfDays,
  referenceBranch,
  type,
}: {
  analysis?: string;
  numberOfDays?: string;
  referenceBranch?: string;
  type?: NewCodeDefinitionType;
}) {
  switch (type) {
    case NewCodeDefinitionType.NumberOfDays:
      return numberOfDays;
    case NewCodeDefinitionType.ReferenceBranch:
      return referenceBranch;
    case NewCodeDefinitionType.SpecificAnalysis:
      return analysis;
    default:
      return undefined;
  }
}

export function validateSetting(state: {
  numberOfDays: string;
  overrideGlobalNewCodeDefinition?: boolean;
  referenceBranch?: string;
  selectedNewCodeDefinitionType?: NewCodeDefinitionType;
}) {
  const {
    numberOfDays,
    overrideGlobalNewCodeDefinition,
    referenceBranch = '',
    selectedNewCodeDefinitionType,
  } = state;

  return (
    overrideGlobalNewCodeDefinition === false ||
    (!!selectedNewCodeDefinitionType &&
      isNewCodeDefinitionCompliant({
        type: selectedNewCodeDefinitionType,
        value: numberOfDays,
      }) &&
      (selectedNewCodeDefinitionType !== NewCodeDefinitionType.ReferenceBranch ||
        referenceBranch.length > 0))
  );
}
