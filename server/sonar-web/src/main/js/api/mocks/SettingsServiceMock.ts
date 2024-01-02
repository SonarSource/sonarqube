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
import { HousekeepingPolicy } from '../../apps/audit-logs/utils';
import { BranchParameters } from '../../types/branch-like';
import { SettingsKey, SettingValue } from '../../types/settings';
import { getAllValues, getValue, getValues } from '../settings';

export default class SettingsServiceMock {
  settingValues: SettingValue[];
  defaultValues: SettingValue[] = [
    {
      key: SettingsKey.AuditHouseKeeping,
      value: HousekeepingPolicy.Weekly,
    },
  ];

  constructor() {
    this.settingValues = cloneDeep(this.defaultValues);
    (getValue as jest.Mock).mockImplementation(this.handleGetValue);
    (getValues as jest.Mock).mockImplementation(this.handleGetValues);
    (getAllValues as jest.Mock).mockImplementation(this.handleGetAllValues);
  }

  handleGetValue = (data: { key: string; component?: string } & BranchParameters) => {
    const setting = this.settingValues.find((s) => s.key === data.key);
    return this.reply(setting);
  };

  handleGetValues = (data: { keys: string[]; component?: string } & BranchParameters) => {
    const settings = this.settingValues.filter((s) => data.keys.includes(s.key));
    return this.reply(settings);
  };

  handleGetAllValues = () => {
    return this.reply(this.settingValues);
  };

  emptySettings = () => {
    this.settingValues = [];
    return this;
  };

  set = (key: SettingsKey, value: string) => {
    const setting = this.settingValues.find((s) => s.key === key);
    if (setting) {
      setting.value = value;
    } else {
      this.settingValues.push({ key, value });
    }
    return this;
  };

  reset = () => {
    this.settingValues = cloneDeep(this.defaultValues);
    return this;
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
