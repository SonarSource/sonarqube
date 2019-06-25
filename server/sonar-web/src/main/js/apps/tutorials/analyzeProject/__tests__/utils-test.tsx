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
import { isAutoScannable } from '../utils';

it('should work for supported languages', () => {
  expect(
    isAutoScannable({
      JavaScript: 512636,
      TypeScript: 425475,
      HTML: 390075,
      CSS: 14099,
      Makefile: 536,
      Dockerfile: 319
    })
  ).toEqual({
    withAllowedLanguages: true,
    withNotAllowedLanguages: false
  });
});

it('should work for non supported languages', () => {
  expect(
    isAutoScannable({
      Java: 434
    })
  ).toEqual({
    withAllowedLanguages: false,
    withNotAllowedLanguages: true
  });
});

it('should work for mixed languages', () => {
  expect(
    isAutoScannable({
      JavaScript: 512636,
      Java: 434
    })
  ).toEqual({
    withAllowedLanguages: true,
    withNotAllowedLanguages: true
  });
});
