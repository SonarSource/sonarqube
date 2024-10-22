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

import { getAllValues } from '../../api/settings';
import { SettingsKey } from '../../types/settings';
import { TokenExpiration } from '../../types/token';
import { mockSettingValue } from '../mocks/settings';
import { mockUserToken } from '../mocks/token';
import {
  computeTokenExpirationDate,
  EXPIRATION_OPTIONS,
  getAvailableExpirationOptions,
  getNextTokenName,
} from '../tokens';

jest.mock('../../api/settings', () => {
  return {
    getAllValues: jest.fn().mockResolvedValue([]),
  };
});

jest.mock('../dates', () => {
  return {
    ...jest.requireActual('../dates'),
    now: jest.fn(() => new Date('2022-06-01T12:00:00Z')),
  };
});

describe('getAvailableExpirationOptions', () => {
  it('should correctly return all options if no setting', async () => {
    expect(await getAvailableExpirationOptions()).toEqual(EXPIRATION_OPTIONS);
  });

  it('should correctly return all options if the max setting is no expiration', async () => {
    (getAllValues as jest.Mock).mockResolvedValueOnce([
      mockSettingValue({ key: SettingsKey.TokenMaxAllowedLifetime, value: 'No expiration' }),
    ]);
    expect(await getAvailableExpirationOptions()).toEqual(EXPIRATION_OPTIONS);
  });

  it('should correctly limit options if the max setting is 1 year', async () => {
    (getAllValues as jest.Mock).mockResolvedValueOnce([
      mockSettingValue({ key: SettingsKey.TokenMaxAllowedLifetime, value: '1 year' }),
    ]);
    expect(await getAvailableExpirationOptions()).toEqual(
      [TokenExpiration.OneMonth, TokenExpiration.ThreeMonths, TokenExpiration.OneYear].map(
        (value) => {
          return {
            value,
            label: `users.tokens.expiration.${value.toString()}`,
          };
        },
      ),
    );
  });

  it('should correctly limit options if the max setting is 3 months', async () => {
    (getAllValues as jest.Mock).mockResolvedValueOnce([
      mockSettingValue({ key: SettingsKey.TokenMaxAllowedLifetime, value: '90 days' }),
    ]);
    expect(await getAvailableExpirationOptions()).toEqual(
      [TokenExpiration.OneMonth, TokenExpiration.ThreeMonths].map((value) => {
        return {
          value,
          label: `users.tokens.expiration.${value.toString()}`,
        };
      }),
    );
  });

  it('should correctly limit options if the max setting is 30 days', async () => {
    (getAllValues as jest.Mock).mockResolvedValueOnce([
      mockSettingValue({ key: SettingsKey.TokenMaxAllowedLifetime, value: '30 days' }),
    ]);
    expect(await getAvailableExpirationOptions()).toEqual([
      {
        value: TokenExpiration.OneMonth,
        label: `users.tokens.expiration.${TokenExpiration.OneMonth.toString()}`,
      },
    ]);
  });
});

describe('computeTokenExpirationDate', () => {
  it.each([
    [TokenExpiration.OneMonth, '2022-07-01'],
    [TokenExpiration.ThreeMonths, '2022-08-30'],
    [TokenExpiration.OneYear, '2023-06-01'],
  ])('should correctly compute the proper expiration date for %s days', (days, expected) => {
    expect(computeTokenExpirationDate(days)).toBe(expected);
  });
});

describe('getNextTokenName', () => {
  it('should preserve the base name for the firts token', () => {
    const tokens = [mockUserToken({ name: 'whatever' })];
    const tokenName = 'sl-vscode';

    expect(getNextTokenName(tokenName, tokens)).toBe(tokenName);
  });

  it('should increment until the first available value', () => {
    const tokenName = 'sl-vscode';
    const tokens = [
      mockUserToken({ name: `${tokenName}` }),
      mockUserToken({ name: `${tokenName}-1` }),
      mockUserToken({ name: `${tokenName}-2` }),
      mockUserToken({ name: `${tokenName}-4` }),
    ];

    expect(getNextTokenName(tokenName, tokens)).toBe(`${tokenName}-3`);
  });
});
