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

import { hasMessage } from '../../../helpers/l10n';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockDefinition, mockSettingValue } from '../../../helpers/mocks/settings';
import {
  ExtendedSettingDefinition,
  Setting,
  SettingFieldDefinition,
  SettingType,
} from '../../../types/settings';
import {
  buildSettingLink,
  getDefaultValue,
  getEmptyValue,
  getPropertyName,
  getSettingValue,
} from '../utils';

jest.mock('../../../helpers/l10n', () => ({
  ...jest.requireActual('../../../helpers/l10n'),
  hasMessage: jest.fn(),
}));

jest.mock('../../../helpers/l10nBundle', () => {
  const bundle = jest.requireActual('../../../helpers/l10nBundle');
  return {
    ...bundle,
    getIntl: () => ({ formatMessage: jest.fn(({ id }) => `${id}`) }),
  };
});

const fields = [
  { key: 'foo', type: 'STRING' } as SettingFieldDefinition,
  { key: 'bar', type: 'SINGLE_SELECT_LIST' } as SettingFieldDefinition,
];

const settingDefinition: ExtendedSettingDefinition = {
  category: 'test',
  fields: [],
  key: 'test',
  options: [],
  subCategory: 'subtest',
};

describe('getPropertyName', () => {
  it('should return correct value for existing translation', () => {
    jest.mocked(hasMessage).mockImplementation(() => true);
    expect(getPropertyName(mockDefinition())).toBe('property.foo.name');
  });

  it('should return a name when translation doesn`t exist', () => {
    jest.mocked(hasMessage).mockImplementation(() => false);
    expect(getPropertyName(mockDefinition({ name: 'nicename', key: 'keey' }))).toBe('nicename');
  });
  it('should return a key when translation and name dont exist', () => {
    expect(getPropertyName(mockDefinition({ key: 'keey' }))).toBe('keey');
  });
});

describe('#getEmptyValue()', () => {
  it('should work for property sets', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.PROPERTY_SET,
      fields,
    };
    expect(getEmptyValue(setting)).toEqual([{ foo: '', bar: null }]);
  });

  it('should work for multi values string', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.STRING,
      multiValues: true,
    };
    expect(getEmptyValue(setting)).toEqual(['']);
  });

  it('should work for multi values boolean', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.BOOLEAN,
      multiValues: true,
    };
    expect(getEmptyValue(setting)).toEqual([null]);
  });
});

describe('#getSettingValue()', () => {
  it('should work for property sets', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.PROPERTY_SET,
      fields,
    };
    const settingValue = mockSettingValue({ fieldValues: [{ foo: '' }] });
    expect(getSettingValue(setting, settingValue)).toEqual([{ foo: '' }]);
  });

  it('should work for category definitions', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.FORMATTED_TEXT,
      fields,
      multiValues: true,
    };
    const settingValue = mockSettingValue({ values: ['*text*', 'text'] });
    expect(getSettingValue(setting, settingValue)).toEqual(['*text*', 'text']);
  });

  it('should work for formatted text', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.FORMATTED_TEXT,
      fields,
    };
    const settingValue = mockSettingValue({ values: ['*text*', 'text'] });
    expect(getSettingValue(setting, settingValue)).toEqual('*text*');
  });

  it('should work for formatted text when values is undefined', () => {
    const setting: ExtendedSettingDefinition = {
      ...settingDefinition,
      type: SettingType.FORMATTED_TEXT,
      fields,
    };
    const settingValue = mockSettingValue({ values: undefined });
    expect(getSettingValue(setting, settingValue)).toBeUndefined();
  });
});

describe('#getDefaultValue()', () => {
  it.each([
    ['true', 'settings.boolean.true'],
    ['false', 'settings.boolean.false'],
  ])(
    'should work for boolean field when passing "%s"',
    (parentValue?: string, expected?: string) => {
      const setting: Setting = {
        hasValue: true,
        definition: { key: 'test', options: [], type: SettingType.BOOLEAN },
        parentValue,
        key: 'test',
      };
      expect(getDefaultValue(setting)).toEqual(expected);
    },
  );
});

describe('buildSettingLink', () => {
  it.each([
    [
      mockDefinition({ key: 'anykey' }),
      undefined,
      { hash: '#anykey', pathname: '/admin/settings', search: '?category=foo+category' },
    ],
    [
      mockDefinition({ key: 'sonar.auth.gitlab.name' }),
      undefined,
      {
        hash: '#sonar.auth.gitlab.name',
        pathname: '/admin/settings',
        search: '?category=foo+category&tab=gitlab',
      },
    ],
    [
      mockDefinition({ key: 'sonar.auth.github.token' }),
      undefined,
      {
        hash: '#sonar.auth.github.token',
        pathname: '/admin/settings',
        search: '?category=foo+category&tab=github',
      },
    ],
    [
      mockDefinition({ key: 'sonar.auth.bitbucket.token' }),
      undefined,
      {
        hash: '#sonar.auth.bitbucket.token',
        pathname: '/admin/settings',
        search: '?category=foo+category&tab=bitbucket',
      },
    ],
    [
      mockDefinition({ key: 'sonar.almintegration.azure' }),
      undefined,
      {
        hash: '#sonar.almintegration.azure',
        pathname: '/admin/settings',
        search: '?category=foo+category&alm=azure',
      },
    ],
    [
      mockDefinition({ key: 'defKey' }),
      mockComponent({ key: 'componentKey' }),
      {
        hash: '#defKey',
        pathname: '/project/settings',
        search: '?id=componentKey&category=foo+category',
      },
    ],
  ])('should work as expected', (definition, component, expectedUrl) => {
    expect(buildSettingLink(definition, component)).toEqual(expectedUrl);
  });
});
