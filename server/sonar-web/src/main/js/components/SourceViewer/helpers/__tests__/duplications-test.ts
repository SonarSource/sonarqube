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
import {
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent
} from '../duplications';

describe('getDuplicationBlocksForIndex', () => {
  it('should return duplications blocks', () => {
    const blocks = [{ _ref: '0', from: 2, size: 2 }];
    expect(getDuplicationBlocksForIndex([{ blocks }], 0)).toBe(blocks);
    expect(getDuplicationBlocksForIndex([{ blocks }], 5)).toEqual([]);
    expect(getDuplicationBlocksForIndex(undefined, 5)).toEqual([]);
  });
});

describe('isDuplicationBlockInRemovedComponent', () => {
  it('should ', () => {
    expect(
      isDuplicationBlockInRemovedComponent([
        { _ref: '0', from: 2, size: 2 },
        { _ref: '0', from: 3, size: 1 }
      ])
    ).toBe(false);
    expect(
      isDuplicationBlockInRemovedComponent([
        { _ref: undefined, from: 2, size: 2 },
        { _ref: '0', from: 3, size: 1 }
      ])
    ).toBe(true);
  });
});
