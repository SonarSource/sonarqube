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
import {
  ExtendedSettingDefinition,
  Setting,
  SettingFieldDefinition,
  SettingType,
  SettingValue,
  SettingWithCategory,
} from '../../types/settings';

export function mockDefinition(
  overrides: Partial<ExtendedSettingDefinition> = {},
): ExtendedSettingDefinition {
  return {
    key: 'foo',
    category: 'foo category',
    fields: [],
    options: [],
    subCategory: 'foo subCat',
    ...overrides,
  };
}

export function mockSettingFieldDefinition(
  overrides: Partial<SettingFieldDefinition> = {},
): SettingFieldDefinition {
  return { key: 'name', name: 'Name', options: [], ...overrides };
}

export function mockSetting(overrides: Partial<Setting> = {}): Setting {
  return {
    key: 'foo',
    value: '42',
    hasValue: true,
    inherited: true,
    definition: {
      key: 'foo',
      name: 'Foo setting',
      description: 'When Foo then Bar',
      type: SettingType.INTEGER,
      options: [],
    },
    ...overrides,
  };
}

export function mockSettingValue(overrides: Partial<SettingValue> = {}) {
  return {
    key: 'test',
    ...overrides,
  };
}

export function mockSettingWithCategory(
  overrides: Partial<SettingWithCategory> = {},
): SettingWithCategory {
  return {
    key: 'foo',
    value: '42',
    hasValue: true,
    inherited: true,
    definition: {
      key: 'foo',
      name: 'Foo setting',
      description: 'When Foo then Bar',
      type: SettingType.INTEGER,
      options: [],
      category: 'general',
      fields: [],
      subCategory: 'email',
    },
    ...overrides,
  };
}
