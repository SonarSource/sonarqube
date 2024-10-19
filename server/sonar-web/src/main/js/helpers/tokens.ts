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
import { getAllValues } from '../api/settings';
import { SettingsKey } from '../types/settings';
import { TokenExpiration, UserToken } from '../types/token';
import { now } from './dates';
import { translate } from './l10n';

export const EXPIRATION_OPTIONS = [
  TokenExpiration.OneMonth,
  TokenExpiration.ThreeMonths,
  TokenExpiration.OneYear,
  TokenExpiration.NoExpiration,
].map((value) => {
  return {
    value,
    label: translate('users.tokens.expiration', value.toString()),
  };
});

const SETTINGS_EXPIRATION_MAP: { [key: string]: TokenExpiration } = {
  '30 days': TokenExpiration.OneMonth,
  '90 days': TokenExpiration.ThreeMonths,
  '1 year': TokenExpiration.OneYear,
  'No expiration': TokenExpiration.NoExpiration,
};

export async function getAvailableExpirationOptions() {
  /*
   * We intentionally fetch all settings, because fetching a specific setting will
   * return it from the DB as a fallback, even if the setting is not defined at startup.
   */
  const setting = (await getAllValues()).find((v) => v.key === SettingsKey.TokenMaxAllowedLifetime);
  if (setting === undefined || setting.value === undefined) {
    return EXPIRATION_OPTIONS;
  }

  const maxTokenLifetime = setting.value;
  if (SETTINGS_EXPIRATION_MAP[maxTokenLifetime] !== TokenExpiration.NoExpiration) {
    return EXPIRATION_OPTIONS.filter(
      (option) =>
        option.value <= SETTINGS_EXPIRATION_MAP[maxTokenLifetime] &&
        option.value !== TokenExpiration.NoExpiration,
    );
  }

  return EXPIRATION_OPTIONS;
}

export function computeTokenExpirationDate(days: number) {
  return computeTokenExpirationDateByHours(days*24);
}

export function computeTokenExpirationDateByHours(hours: number) {
  const expirationDate = now();
  expirationDate.setHours(expirationDate.getHours() + hours);
  return expirationDate.toISOString();
}

export function getNextTokenName(tokenNameBase: string, tokens: UserToken[]) {
  let tokenName = tokenNameBase;
  let counter = 1;
  let existingToken = tokens.find(({ name }) => name === tokenName);
  while (existingToken !== undefined) {
    tokenName = `${tokenNameBase}-${counter}`;
    counter += 1;
    // eslint-disable-next-line no-loop-func
    existingToken = tokens.find(({ name }) => name === tokenName);
  }

  return tokenName;
}
