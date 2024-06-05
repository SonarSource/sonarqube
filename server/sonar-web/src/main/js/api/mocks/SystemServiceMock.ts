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
import { Provider, SysInfoCluster, SysInfoLogging, SysInfoStandalone } from '../../types/types';

import { LogsLevels } from '../../apps/system/utils';
import { mockClusterSysInfo, mockLogs, mockStandaloneSysInfo } from '../../helpers/testMocks';
import { getSystemInfo, getSystemUpgrades, setLogLevel } from '../system';

jest.mock('../system');

type SystemUpgrades = {
  installedVersionActive: boolean;
  latestLTA: string;
  updateCenterRefresh: string;
  upgrades: [];
};

export default class SystemServiceMock {
  isCluster: boolean = false;
  logging: SysInfoLogging = mockLogs();
  systemInfo: SysInfoCluster | SysInfoStandalone = mockStandaloneSysInfo();
  systemUpgrades: SystemUpgrades = {
    upgrades: [],
    latestLTA: '7.9',
    updateCenterRefresh: '2021-09-01',
    installedVersionActive: true,
  };

  constructor() {
    this.updateSystemInfo();
    jest.mocked(getSystemInfo).mockImplementation(this.handleGetSystemInfo);
    jest.mocked(setLogLevel).mockImplementation(this.handleSetLogLevel);
    jest.mocked(getSystemUpgrades).mockImplementation(this.handleGetSystemUpgrades);
  }

  handleGetSystemInfo = () => {
    return this.reply(this.systemInfo);
  };

  handleGetSystemUpgrades = () => {
    return this.reply(this.systemUpgrades);
  };

  setSystemUpgrades(systemUpgrades: Partial<SystemUpgrades>) {
    this.systemUpgrades = {
      ...this.systemUpgrades,
      ...systemUpgrades,
    };
  }

  setProvider(provider: Provider | null) {
    this.systemInfo = mockStandaloneSysInfo({
      ...this.systemInfo,
      System: {
        ...this.systemInfo.System,
        ...(provider ? { 'External Users and Groups Provisioning': provider } : {}),
      },
    });
  }

  handleSetLogLevel = (logsLevel: LogsLevels) => {
    this.logging = mockLogs(logsLevel);
    this.updateSystemInfo();

    return this.reply(undefined);
  };

  updateSystemInfo = () => {
    const logs = {
      'Web Logging': this.logging,
      'Compute Engine Logging': this.logging,
    };

    this.systemInfo = this.isCluster ? mockClusterSysInfo(logs) : mockStandaloneSysInfo(logs);
  };

  setIsCluster = (isCluster: boolean = false) => {
    this.isCluster = isCluster;
    this.updateSystemInfo();
  };

  reset = () => {
    this.logging = mockLogs();
    this.setIsCluster(false);
    this.updateSystemInfo();
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
