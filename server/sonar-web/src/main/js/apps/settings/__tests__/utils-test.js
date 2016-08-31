/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { expect } from 'chai';
import { getEmptyValue } from '../utils';
import { TYPE_PROPERTY_SET, TYPE_STRING, TYPE_SINGLE_SELECT_LIST, TYPE_BOOLEAN } from '../constants';

const fields = [
  { key: 'foo', type: TYPE_STRING },
  { key: 'bar', type: TYPE_SINGLE_SELECT_LIST }
];

describe('Settings :: Utils', () => {
  describe('#getEmptyValue()', () => {
    it('should work for property sets', () => {
      const setting = { type: TYPE_PROPERTY_SET, fields };
      expect(getEmptyValue(setting)).to.deep.equal([{ foo: '', bar: null }]);
    });

    it('should work for multi values string', () => {
      const setting = { type: TYPE_STRING, multiValues: true };
      expect(getEmptyValue(setting)).to.deep.equal(['']);
    });

    it('should work for multi values boolean', () => {
      const setting = { type: TYPE_BOOLEAN, multiValues: true };
      expect(getEmptyValue(setting)).to.deep.equal([null]);
    });
  });
});
