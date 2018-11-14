/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getEmptyValue, getDefaultValue } from '../utils';
import { SettingFieldDefinition, SettingCategoryDefinition, SettingType } from '../../../app/types';

const fields = [
  { key: 'foo', type: SettingType.String } as SettingFieldDefinition,
  { key: 'bar', type: SettingType.SingleSelectList } as SettingFieldDefinition
];

const settingDefinition: SettingCategoryDefinition = {
  category: 'test',
  fields: [],
  key: 'test',
  options: [],
  subCategory: 'subtest'
};

describe('#getEmptyValue()', () => {
  it('should work for property sets', () => {
    const setting = { ...settingDefinition, type: SettingType.PropertySet, fields };
    expect(getEmptyValue(setting)).toEqual([{ foo: '', bar: null }]);
  });

  it('should work for multi values string', () => {
    const setting = { ...settingDefinition, type: SettingType.String, multiValues: true };
    expect(getEmptyValue(setting)).toEqual(['']);
  });

  it('should work for multi values boolean', () => {
    const setting = { ...settingDefinition, type: SettingType.Boolean, multiValues: true };
    expect(getEmptyValue(setting)).toEqual([null]);
  });
});

describe('#getDefaultValue()', () => {
  const check = (parentValue?: string, expected?: string) => {
    const setting = {
      definition: { key: 'test', options: [], type: SettingType.Boolean },
      parentValue,
      key: 'test'
    };
    expect(getDefaultValue(setting)).toEqual(expected);
  };

  it('should work for boolean field when passing "true"', () =>
    check('true', 'settings.boolean.true'));
  it('should work for boolean field when passing "false"', () =>
    check('false', 'settings.boolean.false'));
});
