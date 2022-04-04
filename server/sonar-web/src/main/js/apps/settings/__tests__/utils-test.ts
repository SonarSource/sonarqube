/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
  Setting,
  SettingCategoryDefinition,
  SettingFieldDefinition
} from '../../../types/settings';
import { getDefaultValue, getEmptyValue } from '../utils';

const fields = [
  { key: 'foo', type: 'STRING' } as SettingFieldDefinition,
  { key: 'bar', type: 'SINGLE_SELECT_LIST' } as SettingFieldDefinition
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
    const setting: SettingCategoryDefinition = {
      ...settingDefinition,
      type: 'PROPERTY_SET',
      fields
    };
    expect(getEmptyValue(setting)).toEqual([{ foo: '', bar: null }]);
  });

  it('should work for multi values string', () => {
    const setting: SettingCategoryDefinition = {
      ...settingDefinition,
      type: 'STRING',
      multiValues: true
    };
    expect(getEmptyValue(setting)).toEqual(['']);
  });

  it('should work for multi values boolean', () => {
    const setting: SettingCategoryDefinition = {
      ...settingDefinition,
      type: 'BOOLEAN',
      multiValues: true
    };
    expect(getEmptyValue(setting)).toEqual([null]);
  });
});

describe('#getDefaultValue()', () => {
  it.each([
    ['true', 'settings.boolean.true'],
    ['false', 'settings.boolean.false']
  ])(
    'should work for boolean field when passing "%s"',
    (parentValue?: string, expected?: string) => {
      const setting: Setting = {
        definition: { key: 'test', options: [], type: 'BOOLEAN' },
        parentValue,
        key: 'test'
      };
      expect(getDefaultValue(setting)).toEqual(expected);
    }
  );
});
