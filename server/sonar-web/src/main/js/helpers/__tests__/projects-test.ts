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

import { ProjectKeyValidationResult } from '../../types/component';
import { PROJECT_KEY_MAX_LEN } from '../constants';
import { validateProjectKey } from '../projects';

describe('validateProjectKey', () => {
  it('should correctly flag an invalid key', () => {
    // Cannot have special characters except whitelist.
    expect(validateProjectKey('foo/bar')).toBe(ProjectKeyValidationResult.InvalidChar);
    // Cannot contain only numbers.
    expect(validateProjectKey('123')).toBe(ProjectKeyValidationResult.OnlyDigits);
    // Cannot be more than 400 chars long.
    expect(validateProjectKey(new Array(PROJECT_KEY_MAX_LEN + 1).fill('a').join(''))).toBe(
      ProjectKeyValidationResult.TooLong,
    );
    // Cannot be empty.
    expect(validateProjectKey('')).toBe(ProjectKeyValidationResult.Empty);
  });

  it('should not flag a valid key', () => {
    expect(validateProjectKey('foo:bar_baz-12.is')).toBe(ProjectKeyValidationResult.Valid);
    expect(validateProjectKey('12:34')).toBe(ProjectKeyValidationResult.Valid);
  });
});
