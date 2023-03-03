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
import { NewCodePeriodSettingType } from '../../../../types/types';
import { getSettingValue, validateSetting } from '../../utils';

describe('getSettingValue', () => {
  const state = {
    analysis: 'analysis',
    days: '35',
    referenceBranch: 'branch-4.2',
  };

  it('should work for Days', () => {
    expect(getSettingValue({ ...state, type: NewCodePeriodSettingType.NUMBER_OF_DAYS })).toBe(
      state.days
    );
  });

  it('should work for Analysis', () => {
    expect(getSettingValue({ ...state, type: NewCodePeriodSettingType.SPECIFIC_ANALYSIS })).toBe(
      state.analysis
    );
  });

  it('should work for Previous version', () => {
    expect(
      getSettingValue({ ...state, type: NewCodePeriodSettingType.PREVIOUS_VERSION })
    ).toBeUndefined();
  });

  it('should work for Reference branch', () => {
    expect(getSettingValue({ ...state, type: NewCodePeriodSettingType.REFERENCE_BRANCH })).toBe(
      state.referenceBranch
    );
  });
});

describe('validateSettings', () => {
  it('should validate at branch level', () => {
    expect(validateSetting({ days: '' })).toEqual({ isChanged: false, isValid: false });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.PREVIOUS_VERSION,
        days: '12',
        selected: NewCodePeriodSettingType.NUMBER_OF_DAYS,
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.PREVIOUS_VERSION,
        days: 'nope',
        selected: NewCodePeriodSettingType.NUMBER_OF_DAYS,
      })
    ).toEqual({ isChanged: true, isValid: false });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.NUMBER_OF_DAYS,
        currentSettingValue: '15',
        days: '15',
        selected: NewCodePeriodSettingType.NUMBER_OF_DAYS,
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.NUMBER_OF_DAYS,
        currentSettingValue: '15',
        days: '13',
        selected: NewCodePeriodSettingType.NUMBER_OF_DAYS,
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        analysis: 'analysis1',
        currentSetting: NewCodePeriodSettingType.SPECIFIC_ANALYSIS,
        currentSettingValue: 'analysis1',
        days: '',
        selected: NewCodePeriodSettingType.SPECIFIC_ANALYSIS,
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        analysis: 'analysis2',
        currentSetting: NewCodePeriodSettingType.SPECIFIC_ANALYSIS,
        currentSettingValue: 'analysis1',
        days: '',
        selected: NewCodePeriodSettingType.SPECIFIC_ANALYSIS,
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.REFERENCE_BRANCH,
        currentSettingValue: 'master',
        days: '',
        referenceBranch: 'master',
        selected: NewCodePeriodSettingType.REFERENCE_BRANCH,
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: NewCodePeriodSettingType.REFERENCE_BRANCH,
        currentSettingValue: 'master',
        days: '',
        referenceBranch: '',
        selected: NewCodePeriodSettingType.REFERENCE_BRANCH,
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
        currentSetting: NewCodePeriodSettingType.PREVIOUS_VERSION,
        days: '',
        overrideGeneralSetting: false,
      })
    ).toEqual({
      isChanged: true,
      isValid: true,
    });
  });
});
