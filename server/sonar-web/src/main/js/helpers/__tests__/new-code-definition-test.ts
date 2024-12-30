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

import { NewCodeDefinition, NewCodeDefinitionType } from '../../types/new-code-definition';
import { mockNewCodePeriod } from '../mocks/new-code-definition';
import {
  NUMBER_OF_DAYS_DEFAULT_VALUE,
  getNumberOfDaysDefaultValue,
  isNewCodeDefinitionCompliant,
} from '../new-code-definition';

describe('isNewCodeDefinitionCompliant', () => {
  it.each([
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '0' }), false],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '15' }), true],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '15.' }), false],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '15.0' }), false],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '15.3' }), false],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '91' }), false],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.PreviousVersion }), true],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.ReferenceBranch }), true],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.SpecificAnalysis }), false],
  ])(
    'should test for new code definition compliance properly %s',
    (newCodePeriod: NewCodeDefinition, result: boolean) => {
      expect(isNewCodeDefinitionCompliant(newCodePeriod)).toEqual(result);
    },
  );
});

describe('getNumberOfDaysDefaultValue', () => {
  it.each([
    [null, null, NUMBER_OF_DAYS_DEFAULT_VALUE.toString()],
    [
      mockNewCodePeriod({ type: NewCodeDefinitionType.PreviousVersion }),
      null,
      NUMBER_OF_DAYS_DEFAULT_VALUE.toString(),
    ],
    [
      null,
      mockNewCodePeriod({ type: NewCodeDefinitionType.PreviousVersion }),
      NUMBER_OF_DAYS_DEFAULT_VALUE.toString(),
    ],
    [
      mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '91' }),
      null,
      NUMBER_OF_DAYS_DEFAULT_VALUE.toString(),
    ],
    [
      null,
      mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '91' }),
      NUMBER_OF_DAYS_DEFAULT_VALUE.toString(),
    ],
    [mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '90' }), null, '90'],
    [null, mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '90' }), '90'],
  ])(
    'should return the defaut number of days vale properly %s',
    (
      globalNewCodeDefinition: NewCodeDefinition | null,
      inheritedNewCodeDefinition: NewCodeDefinition | null,
      result: string,
    ) => {
      expect(
        getNumberOfDaysDefaultValue(globalNewCodeDefinition, inheritedNewCodeDefinition),
      ).toEqual(result);
    },
  );
});
