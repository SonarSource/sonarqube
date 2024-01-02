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
import { getSettingValue, validateSetting } from '../../utils';

describe('getSettingValue', () => {
  const state = {
    analysis: 'analysis',
    days: '35',
    referenceBranch: 'branch-4.2',
  };

  it('should work for Days', () => {
    expect(getSettingValue({ ...state, type: 'NUMBER_OF_DAYS' })).toBe(state.days);
  });

  it('should work for Analysis', () => {
    expect(getSettingValue({ ...state, type: 'SPECIFIC_ANALYSIS' })).toBe(state.analysis);
  });

  it('should work for Previous version', () => {
    expect(getSettingValue({ ...state, type: 'PREVIOUS_VERSION' })).toBeUndefined();
  });

  it('should work for Reference branch', () => {
    expect(getSettingValue({ ...state, type: 'REFERENCE_BRANCH' })).toBe(state.referenceBranch);
  });
});

describe('validateSettings', () => {
  it('should validate at branch level', () => {
    expect(validateSetting({ days: '' })).toEqual({ isChanged: false, isValid: false });
    expect(
      validateSetting({
        currentSetting: 'PREVIOUS_VERSION',
        days: '12',
        selected: 'NUMBER_OF_DAYS',
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        currentSetting: 'PREVIOUS_VERSION',
        days: 'nope',
        selected: 'NUMBER_OF_DAYS',
      })
    ).toEqual({ isChanged: true, isValid: false });
    expect(
      validateSetting({
        currentSetting: 'NUMBER_OF_DAYS',
        currentSettingValue: '15',
        days: '15',
        selected: 'NUMBER_OF_DAYS',
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: 'NUMBER_OF_DAYS',
        currentSettingValue: '15',
        days: '13',
        selected: 'NUMBER_OF_DAYS',
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        analysis: 'analysis1',
        currentSetting: 'SPECIFIC_ANALYSIS',
        currentSettingValue: 'analysis1',
        days: '',
        selected: 'SPECIFIC_ANALYSIS',
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        analysis: 'analysis2',
        currentSetting: 'SPECIFIC_ANALYSIS',
        currentSettingValue: 'analysis1',
        days: '',
        selected: 'SPECIFIC_ANALYSIS',
      })
    ).toEqual({ isChanged: true, isValid: true });
    expect(
      validateSetting({
        currentSetting: 'REFERENCE_BRANCH',
        currentSettingValue: 'master',
        days: '',
        referenceBranch: 'master',
        selected: 'REFERENCE_BRANCH',
      })
    ).toEqual({ isChanged: false, isValid: true });
    expect(
      validateSetting({
        currentSetting: 'REFERENCE_BRANCH',
        currentSettingValue: 'master',
        days: '',
        referenceBranch: '',
        selected: 'REFERENCE_BRANCH',
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
        currentSetting: 'PREVIOUS_VERSION',
        days: '',
        overrideGeneralSetting: false,
      })
    ).toEqual({
      isChanged: true,
      isValid: true,
    });
  });
});
