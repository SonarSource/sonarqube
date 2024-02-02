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
import { cloneDeep } from 'lodash';
import { mockSettingValue } from '../../helpers/mocks/settings';
import { BranchParameters } from '../../types/branch-like';
import { SettingDefinition, SettingValue } from '../../types/settings';
import { getValues, resetSettingValue, setSettingValue } from '../settings';

export default class AuthenticationServiceMock {
  settingValues: SettingValue[];
  defaulSettingValues: SettingValue[] = [
    mockSettingValue({ key: 'test1', value: '' }),
    mockSettingValue({ key: 'test2', value: 'test2' }),
    mockSettingValue({ key: 'sonar.auth.saml.certificate.secured' }),
    mockSettingValue({ key: 'sonar.auth.saml.enabled', value: 'false' }),
    mockSettingValue({ key: 'sonar.auth.github.enabled', value: 'true' }),
    mockSettingValue({ key: 'sonar.auth.github.allowUsersToSignUp', value: 'true' }),
    mockSettingValue({ key: 'sonar.auth.gitlab.enabled', value: 'true' }),
    mockSettingValue({ key: 'sonar.auth.gitlab.allowUsersToSignUp', value: 'true' }),
    mockSettingValue({ key: 'sonar.auth.bitbucket.enabled', value: 'true' }),
    mockSettingValue({ key: 'sonar.auth.bitbucket.allowUsersToSignUp', value: 'true' }),
  ];

  constructor() {
    this.settingValues = cloneDeep(this.defaulSettingValues);
    (getValues as jest.Mock).mockImplementation(this.getValuesHandler);
    (setSettingValue as jest.Mock).mockImplementation(this.setValueHandler);
    (resetSettingValue as jest.Mock).mockImplementation(this.resetValueHandler);
  }

  getValuesHandler = (data: { keys: string; component?: string } & BranchParameters) => {
    if (data.keys) {
      return Promise.resolve(this.settingValues.filter((set) => data.keys.includes(set.key)));
    }
    return Promise.resolve(this.settingValues);
  };

  setValueHandler = (definition: SettingDefinition, value: string) => {
    if (value === 'error') {
      const res = new Response('', {
        status: 400,
        statusText: 'fail',
      });

      return Promise.reject(res);
    }
    const updatedSettingValue = this.settingValues.find((set) => set.key === definition.key);
    if (updatedSettingValue) {
      updatedSettingValue.value = value;
    }
    return Promise.resolve();
  };

  resetValueHandler = (data: { keys: string; component?: string } & BranchParameters) => {
    if (data.keys) {
      this.settingValues.forEach((set) => {
        if (data.keys.includes(set.key)) {
          set.value = '';
        }
        return set;
      });
    }
    return Promise.resolve();
  };

  resetValues = () => {
    this.settingValues = cloneDeep(this.defaulSettingValues);
  };
}
