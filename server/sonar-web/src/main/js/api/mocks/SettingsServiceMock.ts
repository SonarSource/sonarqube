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

import { cloneDeep, isArray, isObject, isString } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { HousekeepingPolicy } from '../../apps/audit-logs/utils';
import { mockDefinition, mockSettingFieldDefinition } from '../../helpers/mocks/settings';
import { isDefined } from '../../helpers/types';
import {
  ExtendedSettingDefinition,
  SettingDefinition,
  SettingType,
  SettingValue,
  SettingsKey,
} from '../../types/settings';
import { Dict } from '../../types/types';
import {
  checkSecretKey,
  encryptValue,
  generateSecretKey,
  getAllValues,
  getDefinitions,
  getValue,
  getValues,
  resetSettingValue,
  setSettingValue,
  setSimpleSettingValue,
} from '../settings';

jest.mock('../settings');

const isEmptyField = (o: any) => isObject(o) && Object.values(o).some(isEmptyString);
const isEmptyString = (i: any) => isString(i) && i.trim() === '';

export const DEFAULT_DEFINITIONS_MOCK = [
  mockDefinition({
    category: 'general',
    key: 'sonar.announcement.message',
    subCategory: 'Announcement',
    name: 'Announcement message',
    description: 'Enter message',
    type: SettingType.TEXT,
  }),
  mockDefinition({
    category: 'general',
    key: 'sonar.ce.parallelProjectTasks',
    subCategory: 'Compute Engine',
    name: 'Run analysis in paralel',
    description: 'Enter message',
    type: SettingType.TEXT,
  }),
  mockDefinition({
    category: 'javascript',
    key: 'sonar.javascript.globals',
    subCategory: 'General',
    name: 'Global Variables',
    description: 'List of Global variables',
    multiValues: true,
    defaultValue: 'angular,google,d3',
  }),
  mockDefinition({
    category: 'javascript',
    key: 'sonar.javascript.file.suffixes',
    subCategory: 'General',
    name: 'JavaScript File Suffixes',
    description: 'List of suffixes for files to analyze',
    multiValues: true,
    defaultValue: '.js,.jsx,.cjs,.vue,.mjs',
  }),
  mockDefinition({
    category: 'External Analyzers',
    key: 'sonar.androidLint.reportPaths',
    subCategory: 'Android',
    name: 'Android Lint Report Files',
    description: 'Paths to xml files',
    multiValues: true,
  }),
  mockDefinition({
    category: 'COBOL',
    key: 'sonar.cobol.compilationConstants',
    subCategory: 'Preprocessor',
    name: 'Compilation Constants',
    description: 'Lets do it',
    type: SettingType.PROPERTY_SET,
    fields: [
      mockSettingFieldDefinition(),
      mockSettingFieldDefinition({ key: 'value', name: 'Value' }),
    ],
  }),
  mockDefinition({
    category: 'authentication',
    defaultValue: 'true',
    key: 'sonar.auth.github.allowUsersToSignUp',
    subCategory: 'github',
    name: 'Compilation Constants',
    description: 'Lets do it',
    type: SettingType.BOOLEAN,
  }),
  mockDefinition({
    category: 'authentication',
    defaultValue: 'false',
    key: 'provisioning.github.project.visibility.enabled',
    subCategory: 'github',
    name: 'Compilation Constants',
    description: 'Lets do it',
    type: SettingType.BOOLEAN,
  }),
  mockDefinition({
    category: 'Mode',
    defaultValue: 'true',
    key: 'sonar.multi-quality-mode.enabled',
    name: 'Enable Multi-Quality Rule Mode',
    options: [],
    subCategory: 'Mode',
    type: SettingType.BOOLEAN,
  }),
];

export default class SettingsServiceMock {
  #defaultValues: SettingValue[] = [
    {
      key: SettingsKey.AuditHouseKeeping,
      value: HousekeepingPolicy.Weekly,
    },
    {
      key: 'sonar.javascript.globals',
      values: ['angular', 'google', 'd3'],
    },
    {
      key: SettingsKey.QPAdminCanDisableInheritedRules,
      value: 'true',
    },
    {
      key: SettingsKey.MQRMode,
      value: 'true',
    },
  ];

  #settingValues: SettingValue[] = cloneDeep(this.#defaultValues);

  #definitions: ExtendedSettingDefinition[] = cloneDeep(DEFAULT_DEFINITIONS_MOCK);

  #secretKeyAvailable: boolean = false;

  constructor() {
    jest.mocked(getDefinitions).mockImplementation(this.handleGetDefinitions);
    jest.mocked(getValue).mockImplementation(this.handleGetValue);
    jest.mocked(getValues).mockImplementation(this.handleGetValues);
    jest.mocked(getAllValues).mockImplementation(this.handleGetAllValues);
    jest.mocked(setSettingValue).mockImplementation(this.handleSetSettingValue);
    jest.mocked(setSimpleSettingValue).mockImplementation(this.handleSetSimpleSettingValue);
    jest.mocked(resetSettingValue).mockImplementation(this.handleResetSettingValue);
    jest.mocked(checkSecretKey).mockImplementation(this.handleCheckSecretKey);
    jest.mocked(generateSecretKey).mockImplementation(this.handleGenerateSecretKey);
    jest.mocked(encryptValue).mockImplementation(this.handleEcnryptValue);
  }

  handleGetValue = (data: { component?: string; key: string } & BranchParameters) => {
    const setting = this.#settingValues.find((s) => s.key === data.key) as SettingValue;
    const definition = this.#definitions.find(
      (d) => d.key === data.key,
    ) as ExtendedSettingDefinition;
    if (!setting && definition?.defaultValue !== undefined) {
      const fields = definition.multiValues
        ? { values: definition.defaultValue?.split(',') }
        : { value: definition.defaultValue };
      return this.reply({ key: data.key, ...fields });
    }
    return this.reply(setting ?? undefined);
  };

  handleGetValues = (data: { component?: string; keys: string[] } & BranchParameters) => {
    const settings = data.keys
      .map((k) => {
        const def = this.#definitions.find((d) => d.key === k);
        const v = this.#settingValues.find((s) => s.key === k);
        if (v === undefined && def?.type === SettingType.BOOLEAN) {
          return { key: k, value: def.defaultValue, inherited: true };
        }
        return v;
      })
      .filter(isDefined);

    return this.reply(settings);
  };

  handleGetAllValues = () => {
    return this.reply(this.#settingValues);
  };

  handleGetDefinitions = () => {
    return this.reply(this.#definitions);
  };

  handleSetSettingValue = (definition: SettingDefinition, value: any): Promise<void> => {
    if (
      isEmptyString(value) ||
      (isArray(value) && value.some(isEmptyString)) ||
      isEmptyField(value)
    ) {
      throw new ResponseError('validation error', {
        errors: [{ msg: 'A non empty value must be provided' }],
      });
    }

    this.set(definition.key, value);
    const def = this.#definitions.find((d) => d.key === definition.key);
    if (def === undefined) {
      this.#definitions.push(definition as ExtendedSettingDefinition);
    }

    return this.reply(undefined);
  };

  handleResetSettingValue = (data: { component?: string; keys: string } & BranchParameters) => {
    const setting = this.#settingValues.find((s) => s.key === data.keys) as SettingValue;
    const definition = this.#definitions.find(
      (d) => d.key === data.keys,
    ) as ExtendedSettingDefinition;
    if (data.keys === 'sonar.auth.github.userConsentForPermissionProvisioningRequired') {
      this.#settingValues = this.#settingValues.filter(
        (s) => s.key !== 'sonar.auth.github.userConsentForPermissionProvisioningRequired',
      );
    } else if (data.keys === 'sonar.auth.gitlab.userConsentForPermissionProvisioningRequired') {
      this.#settingValues = this.#settingValues.filter(
        (s) => s.key !== 'sonar.auth.gitlab.userConsentForPermissionProvisioningRequired',
      );
    } else if (definition.type === SettingType.PROPERTY_SET) {
      const fieldValues: Dict<string>[] = [];
      if (setting) {
        setting.fieldValues = fieldValues;
      } else {
        this.#settingValues.push({ key: data.keys, fieldValues });
      }
    } else if (definition.multiValues === true) {
      const values = definition.defaultValue?.split(',') ?? [];
      if (setting) {
        setting.values = values;
      } else {
        this.#settingValues.push({ key: data.keys, values });
      }
    } else {
      const value = definition.defaultValue ?? '';
      if (setting) {
        setting.value = value;
      } else {
        this.#settingValues.push({ key: data.keys, value });
      }
    }

    return this.reply(undefined);
  };

  emptySettings = () => {
    this.#settingValues = [];
    return this;
  };

  set = (key: string | SettingsKey, value: any) => {
    const setting = this.#settingValues.find((s) => s.key === key);
    if (setting) {
      setting.value = String(value);
      setting.values = value;
      setting.fieldValues = value;
    } else {
      this.#settingValues.push({ key, value: String(value), values: value, fieldValues: value });
    }
    return this;
  };

  setDefinition = (definition: ExtendedSettingDefinition) => {
    this.#definitions.push(definition);
  };

  setDefinitions = (definitions: ExtendedSettingDefinition[]) => {
    this.#definitions = definitions;
  };

  handleCheckSecretKey = () => {
    return this.reply({ secretKeyAvailable: this.#secretKeyAvailable });
  };

  handleGenerateSecretKey = () => {
    return this.reply({ secretKey: 'secretKey' });
  };

  handleEcnryptValue = () => {
    return this.reply({ encryptedValue: 'encryptedValue' });
  };

  handleSetSimpleSettingValue: typeof setSimpleSettingValue = (data) => {
    this.set(data.key, data.value);
    return this.reply(undefined);
  };

  setSecretKeyAvailable = (val = false) => {
    this.#secretKeyAvailable = val;
  };

  reset = () => {
    this.#settingValues = cloneDeep(this.#defaultValues);
    this.#definitions = cloneDeep(DEFAULT_DEFINITIONS_MOCK);
    this.#secretKeyAvailable = false;
    return this;
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}

class ResponseError extends Error {
  #response: any;
  constructor(name: string, response: any) {
    super(name);
    this.#response = response;
  }

  json = () => Promise.resolve(this.#response);
}
