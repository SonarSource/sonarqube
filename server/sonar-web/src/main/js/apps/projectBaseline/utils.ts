/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
export function validateDays(days: string) {
  const parsed = parseInt(days, 10);

  return !(days.length < 1 || isNaN(parsed) || parsed < 1 || String(parsed) !== days);
}

export function getSettingValue({
  analysis,
  days,
  type
}: {
  analysis?: string;
  days?: string;
  type?: T.NewCodePeriodSettingType;
}) {
  switch (type) {
    case 'NUMBER_OF_DAYS':
      return days;
    case 'SPECIFIC_ANALYSIS':
      return analysis;
    default:
      return undefined;
  }
}

export function validateSetting(state: {
  analysis?: string;
  currentSetting?: T.NewCodePeriodSettingType;
  currentSettingValue?: string;
  days: string;
  selected?: T.NewCodePeriodSettingType;
  overrideGeneralSetting?: boolean;
}) {
  const {
    analysis = '',
    currentSetting,
    currentSettingValue,
    days,
    selected,
    overrideGeneralSetting
  } = state;

  let isChanged;
  if (!currentSetting && overrideGeneralSetting !== undefined) {
    isChanged = overrideGeneralSetting;
  } else {
    isChanged =
      overrideGeneralSetting === false ||
      selected !== currentSetting ||
      (selected === 'NUMBER_OF_DAYS' && days !== currentSettingValue) ||
      (selected === 'SPECIFIC_ANALYSIS' && analysis !== currentSettingValue);
  }

  const isValid =
    overrideGeneralSetting === false ||
    selected === 'PREVIOUS_VERSION' ||
    (selected === 'SPECIFIC_ANALYSIS' && analysis.length > 0) ||
    (selected === 'NUMBER_OF_DAYS' && validateDays(days));

  return { isChanged, isValid };
}
