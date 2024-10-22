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

import { cloneDeep, omit } from 'lodash';
import {
  mockAvailablePlugin,
  mockInstalledPlugin,
  mockRelease,
  mockUpdate,
} from '../../helpers/mocks/plugins';
import { AvailablePlugin, InstalledPlugin, PendingPluginResult } from '../../types/plugins';
import {
  cancelPendingPlugins,
  getAvailablePlugins,
  getInstalledPlugins,
  getPendingPlugins,
  getUpdatesPlugins,
  installPlugin,
  uninstallPlugin,
  updatePlugin,
} from '../plugins';

jest.mock('../plugins');

const defaultAvailable: AvailablePlugin[] = [
  mockAvailablePlugin({
    key: 'foo',
    name: 'CFoo',
    category: 'Languages',
    description: 'Description',
    homepageUrl: 'https://www.sonarsource.com/',
    issueTrackerUrl: 'https://www.sonarsource.com/',
    organizationName: 'SonarSource',
    organizationUrl: 'https://www.sonarsource.com/',
    release: mockRelease({
      version: '1.0.0',
      description: 'release description',
      date: '2020-01-01',
      changeLogUrl: 'https://www.sonarsource.com/',
    }),
  }),
  mockAvailablePlugin({
    key: 'bar',
    name: 'DTest',
    editionBundled: true,
    release: mockRelease({
      version: '2.0.0',
    }),
  }),
  mockAvailablePlugin({
    description: 'Mocked Plugin',
    termsAndConditionsUrl: 'https://www.sonarsource.com/',
  }),
];
const defaultInstalled: InstalledPlugin[] = [
  mockInstalledPlugin({
    key: 'test',
    name: 'ATest_install',
    version: '1.1.0',
  }),
  mockInstalledPlugin({
    key: 'one-update',
    name: 'ZTest',
    version: '1.1.1',
    updates: [
      mockUpdate({
        status: 'COMPATIBLE',
        release: mockRelease({
          version: '1.2.0',
          date: '2020-01-01',
          changeLogUrl: 'https://www.sonarsource.com/',
        }),
      }),
    ],
  }),
  mockInstalledPlugin({
    key: 'multiple_updates',
    name: 'BTest_update',
    version: '1.2.0',
    editionBundled: true,
    updates: [
      mockUpdate({
        status: 'COMPATIBLE',
        release: mockRelease({ version: '1.2.1', changeLogUrl: 'https://www.sonarsource.com/' }),
      }),
      mockUpdate({
        status: 'COMPATIBLE',
        release: mockRelease({ version: '1.3.0', changeLogUrl: 'https://www.sonarsource.com/' }),
      }),
      mockUpdate({
        status: 'NON-COMPATIBLE',
        release: mockRelease({ version: '1.4.0', changeLogUrl: 'https://www.sonarsource.com/' }),
      }),
    ],
  }),
];
const defaultPending: PendingPluginResult = {
  installing: [],
  removing: [],
  updating: [],
};

export default class PluginsServiceMock {
  #available: AvailablePlugin[];
  #installed: InstalledPlugin[];
  #pending: PendingPluginResult;

  constructor() {
    this.#available = cloneDeep(defaultAvailable);
    this.#installed = cloneDeep(defaultInstalled);
    this.#pending = cloneDeep(defaultPending);

    jest.mocked(getAvailablePlugins).mockImplementation(this.handleGetAvailablePlugins);
    jest.mocked(getPendingPlugins).mockImplementation(this.handleGetPendingPlugins);
    jest.mocked(getInstalledPlugins).mockImplementation(this.handleGetInstalledPlugins);
    jest.mocked(getUpdatesPlugins).mockImplementation(this.handleGetUpdatesPlugins);
    jest.mocked(installPlugin).mockImplementation(this.handleInstallPlugin);
    jest.mocked(uninstallPlugin).mockImplementation(this.handleUninstallPlugin);
    jest.mocked(updatePlugin).mockImplementation(this.handleUpdatePlugin);
    jest.mocked(cancelPendingPlugins).mockImplementation(this.handleCancelPendingPlugins);
  }

  handleGetAvailablePlugins = () => {
    return this.reply({
      plugins: this.#available,
      updateCenterRefresh: '2021-01-01T00:00:00+0000',
    });
  };

  handleGetPendingPlugins = () => {
    return this.reply(this.#pending);
  };

  handleGetInstalledPlugins = () => {
    return this.reply(this.#installed.map((plugin) => omit(plugin, 'updates')));
  };

  handleGetUpdatesPlugins = () => {
    return this.reply({
      plugins: this.#installed
        .filter((plugin) => plugin.updates && plugin.updates.length > 0)
        .map((plugin) => omit(plugin, 'version', 'updatedAt')),
      updateCenterRefresh: '2021-01-01T00:00:00+0000',
    });
  };

  handleGetPluginUpdates = () => {
    return this.reply(
      this.#installed.filter((plugin) => plugin.updates && plugin.updates.length > 0),
    );
  };

  handleInstallPlugin: typeof installPlugin = (data) => {
    const plugin = this.#available.find((plugin) => plugin.key === data.key);
    if (plugin === undefined) {
      return Promise.reject(new Error('Plugin not found'));
    }
    this.#pending.installing.push({
      ...omit(plugin, 'release', 'update'),
      version: plugin.release.version,
      implementationBuild: '20210101-000000',
    });
    return this.reply();
  };

  handleUninstallPlugin: typeof uninstallPlugin = (data) => {
    const plugin = this.#installed.find((plugin) => plugin.key === data.key);
    if (plugin === undefined) {
      return Promise.reject(new Error('Plugin not found'));
    }
    this.#pending.removing.push({
      ...omit(
        plugin,
        'updates',
        'updatedAt',
        'sonarLintSupported',
        'hash',
        'filename',
        'documentationPath',
      ),
      implementationBuild: '20210101-000000',
    });
    return this.reply();
  };

  handleUpdatePlugin: typeof updatePlugin = (data) => {
    const plugin = this.#installed
      .filter((plugin) => plugin.updates && plugin.updates.length > 0)
      .find((plugin) => plugin.key === data.key);
    if (plugin === undefined || plugin.updates === undefined || plugin.updates.length === 0) {
      return Promise.reject(new Error('Plugin not found'));
    }
    this.#pending.updating.push({
      ...omit(
        plugin,
        'updates',
        'updatedAt',
        'sonarLintSupported',
        'hash',
        'filename',
        'documentationPath',
      ),
      version: plugin.updates[plugin.updates.length - 1].release?.version ?? '',
      implementationBuild: '20210101-000000',
    });
    return this.reply();
  };

  handleCancelPendingPlugins: typeof cancelPendingPlugins = () => {
    this.#pending = cloneDeep(defaultPending);
    return this.reply();
  };

  reset = () => {
    this.#available = cloneDeep(defaultAvailable);
    this.#installed = cloneDeep(defaultInstalled);
    this.#pending = cloneDeep(defaultPending);
  };

  reply<T>(): Promise<void>;
  reply<T>(response: T): Promise<T>;
  reply<T>(response?: T): Promise<T | void> {
    return Promise.resolve(response ? cloneDeep(response) : undefined);
  }
}
