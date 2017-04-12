/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { highlightSymbol } from '../highlight';

describe('highlightSymbol', () => {
  it('should not highlight symbols with similar beginning', () => {
    // test all positions of sym-X in the string: beginning, middle and ending
    const tokens = [
      { className: 'sym-18 b', text: 'foo' },
      { className: 'a sym-18', text: 'foo' },
      { className: 'a sym-18 b', text: 'foo' },
      { className: 'sym-1 d', text: 'bar' },
      { className: 'c sym-1', text: 'bar' },
      { className: 'c sym-1 d', text: 'bar' }
    ];
    expect(highlightSymbol(tokens, 'sym-1')).toEqual([
      { className: 'sym-18 b', text: 'foo' },
      { className: 'a sym-18', text: 'foo' },
      { className: 'a sym-18 b', text: 'foo' },
      { className: 'sym-1 d highlighted', text: 'bar' },
      { className: 'c sym-1 highlighted', text: 'bar' },
      { className: 'c sym-1 d highlighted', text: 'bar' }
    ]);
  });

  it('should highlight symbols marked twice', () => {
    const tokens = [
      { className: 'sym sym-1 sym sym-2', text: 'foo' },
      { className: 'sym sym-1', text: 'bar' },
      { className: 'sym sym-2', text: 'qux' }
    ];
    expect(highlightSymbol(tokens, 'sym-1')).toEqual([
      { className: 'sym sym-1 sym sym-2 highlighted', text: 'foo' },
      { className: 'sym sym-1 highlighted', text: 'bar' },
      { className: 'sym sym-2', text: 'qux' }
    ]);
  });
});
