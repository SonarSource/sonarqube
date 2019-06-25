/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import { getUniqueTokenName } from '../utils';

const initialTokenName = 'Analyze "lightsaber"';

it('should return the given name when the user has no token', () => {
  const userTokens: T.UserToken[] = [];

  expect(getUniqueTokenName(userTokens, initialTokenName)).toBe(initialTokenName);
});

it('should generate a token with the given name', () => {
  const userTokens = [{ name: initialTokenName, createdAt: '2019-06-14T09:45:52+0200' }];

  expect(getUniqueTokenName(userTokens, 'Analyze "project"')).toBe('Analyze "project"');
});

it('should generate a unique token when the name already exists', () => {
  const userTokens = [
    { name: initialTokenName, createdAt: '2019-06-14T09:45:52+0200' },
    { name: `${initialTokenName} 1`, createdAt: '2019-06-14T09:45:52+0200' }
  ];

  expect(getUniqueTokenName(userTokens, initialTokenName)).toBe('Analyze "lightsaber" 2');
});
