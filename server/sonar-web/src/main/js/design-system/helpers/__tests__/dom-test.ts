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

import { findAnchor } from '../dom';

it('should find the correct anchor', () => {
  const targetRect = new DOMRect(20, 20, 10, 10);

  let rect = new DOMRect(25, 0, 30, 10);
  expect(findAnchor(rect, targetRect, 8)).toStrictEqual({
    left: -1.5,
    rotate: '-90deg',
    top: 16,
    width: 6,
  });

  rect = new DOMRect(35, 25, 30, 10);
  expect(findAnchor(rect, targetRect, 8)).toStrictEqual({
    left: -9,
    rotate: '0deg',
    top: -1.5,
    width: 1,
  });

  rect = new DOMRect(25, 35, 30, 10);
  expect(findAnchor(rect, targetRect, 8)).toStrictEqual({
    left: -1.5,
    rotate: '90deg',
    top: -9,
    width: 1,
  });

  rect = new DOMRect(0, 25, 10, 30);
  expect(findAnchor(rect, targetRect, 8)).toStrictEqual({
    left: 24,
    rotate: '180deg',
    top: -1.5,
    width: 14,
  });
});
