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
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import { getSettingValue, validateSetting } from '../../utils';

describe('getSettingValue', () => {
  const state = {
    analysis: 'analysis',
    days: '35',
    referenceBranch: 'branch-4.2',
  };

  it('should work for Days', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.NumberOfDays })).toBe(
      state.days
    );
  });

  it('should work for Analysis', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.SpecificAnalysis })).toBe(
      state.analysis
    );
  });

  it('should work for Previous version', () => {
    expect(
      getSettingValue({ ...state, type: NewCodeDefinitionType.PreviousVersion })
    ).toBeUndefined();
  });

  it('should work for Reference branch', () => {
    expect(getSettingValue({ ...state, type: NewCodeDefinitionType.ReferenceBranch })).toBe(
      state.referenceBranch
    );
  });
});

describe('validateSettings', () => {
  it('should validate at branch level', () => {
    expect(validateSetting({ days: '' })).toEqual({ isChanged: false, isValid: false });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.PreviousVersion,
        days: '12',
        selected: NewCodeDefinitionType.NumberOfDays,
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.PreviousVersion,
        days: 'nope',
        selected: NewCodeDefinitionType.NumberOfDays,
      })
    ).toEqual({ isChanged: true, isValid: false });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.NumberOfDays,
        currentSettingValue: '15',
        days: '15',
        selected: NewCodeDefinitionType.NumberOfDays,
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.NumberOfDays,
        currentSettingValue: '15',
        days: '13',
        selected: NewCodeDefinitionType.NumberOfDays,
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        analysis: 'analysis1',
        currentSetting: NewCodeDefinitionType.SpecificAnalysis,
        currentSettingValue: 'analysis1',
        days: '',
        selected: NewCodeDefinitionType.SpecificAnalysis,
      })
    ).toEqual({ isChanged: false, isValid: false });
    expect(
      validateSetting({
        analysis: 'analysis2',
        currentSetting: NewCodeDefinitionType.SpecificAnalysis,
        currentSettingValue: 'analysis1',
        days: '',
        selected: NewCodeDefinitionType.SpecificAnalysis,
      })
    ).toEqual({ isChanged: true, isValid: false });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.ReferenceBranch,
        currentSettingValue: 'master',
        days: '',
        referenceBranch: 'master',
        selected: NewCodeDefinitionType.ReferenceBranch,
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.ReferenceBranch,
        currentSettingValue: 'master',
        days: '',
        referenceBranch: '',
        selected: NewCodeDefinitionType.ReferenceBranch,
      })
    ).toEqual({ isChanged: true, isValid: false });
  });

  it('should validate at project level', () => {
    expect(validateSetting({ days: '', overrideGeneralSetting: false })).toEqual({
      isChanged: false,
      isValid: true,
    });
    expect(validateSetting({ days: '', overrideGeneralSetting: true })).toEqual({
      isChanged: true,
      isValid: false,
    });
    expect(
      validateSetting({
        currentSetting: NewCodeDefinitionType.PreviousVersion,
        days: '',
        overrideGeneralSetting: false,
      })
    ).toEqual({
      isChanged: true,
      isValid: true,
    });
  });
});
