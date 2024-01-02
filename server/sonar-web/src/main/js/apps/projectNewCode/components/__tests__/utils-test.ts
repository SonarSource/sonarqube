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
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import { getSettingValue, validateSetting } from '../../utils';

describe('getSettingValue', () => {
  const state = {
    analysis: 'analysis',
    numberOfDays: '35',
    referenceBranch: 'branch-4.2',
  };

  it('should work for Days', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.NumberOfDays })).toBe(
      state.numberOfDays,
    );
  });

  it('should work for Analysis', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.SpecificAnalysis })).toBe(
      state.analysis,
    );
  });

  it('should work for Previous version', () => {
    expect(
      getSettingValue({ ...state, type: NewCodeDefinitionType.PreviousVersion }),
    ).toBeUndefined();
  });

  it('should work for Reference branch', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.ReferenceBranch })).toBe(
      state.referenceBranch,
    );
  });
});

describe('validateSettings', () => {
  it('should validate at branch level', () => {
    expect(validateSetting({ numberOfDays: '' })).toEqual(false);
    expect(
      validateSetting({
        numberOfDays: '12',
        selectedNewCodeDefinitionType: NewCodeDefinitionType.NumberOfDays,
      }),
    ).toEqual(true);
    expect(
      validateSetting({
        numberOfDays: 'nope',
        selectedNewCodeDefinitionType: NewCodeDefinitionType.NumberOfDays,
      }),
    ).toEqual(false);
    expect(
      validateSetting({
        numberOfDays: '',
        selectedNewCodeDefinitionType: NewCodeDefinitionType.SpecificAnalysis,
      }),
    ).toEqual(false);
    expect(
      validateSetting({
        numberOfDays: '',
        referenceBranch: 'master',
        selectedNewCodeDefinitionType: NewCodeDefinitionType.ReferenceBranch,
      }),
    ).toEqual(true);
    expect(
      validateSetting({
        numberOfDays: '',
        referenceBranch: '',
        selectedNewCodeDefinitionType: NewCodeDefinitionType.ReferenceBranch,
      }),
    ).toEqual(false);
  });

  it('should validate at project level', () => {
    expect(validateSetting({ numberOfDays: '', overrideGlobalNewCodeDefinition: false })).toEqual(
      true,
    );
    expect(
      validateSetting({
        selectedNewCodeDefinitionType: NewCodeDefinitionType.PreviousVersion,
        numberOfDays: '',
        overrideGlobalNewCodeDefinition: true,
      }),
    ).toEqual(true);
    expect(
      validateSetting({
        selectedNewCodeDefinitionType: NewCodeDefinitionType.NumberOfDays,
        numberOfDays: '',
        overrideGlobalNewCodeDefinition: true,
      }),
    ).toEqual(false);
    expect(
      validateSetting({
        selectedNewCodeDefinitionType: NewCodeDefinitionType.NumberOfDays,
        numberOfDays: '12',
        overrideGlobalNewCodeDefinition: true,
      }),
    ).toEqual(true);
  });
});
