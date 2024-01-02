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
import { mockAlmSettingsInstance } from '../../helpers/mocks/alm-settings';
import { AlmKeys, AlmSettingsInstance } from '../../types/alm-settings';
import { getAlmSettings } from '../alm-settings';

export default class AlmSettingsServiceMock {
  almSettings: AlmSettingsInstance[];
  defaultSetting: AlmSettingsInstance[] = [
    mockAlmSettingsInstance({ key: 'conf-final-1', alm: AlmKeys.GitLab }),
    mockAlmSettingsInstance({ key: 'conf-final-2', alm: AlmKeys.GitLab }),
    mockAlmSettingsInstance({ key: 'conf-github-1', alm: AlmKeys.GitHub, url: 'http://url' }),
    mockAlmSettingsInstance({ key: 'conf-github-2', alm: AlmKeys.GitHub, url: 'http://url' }),
    mockAlmSettingsInstance({ key: 'conf-github-3', alm: AlmKeys.GitHub, url: 'javascript://url' }),
    mockAlmSettingsInstance({ key: 'conf-azure-1', alm: AlmKeys.Azure, url: 'url' }),
    mockAlmSettingsInstance({ key: 'conf-azure-2', alm: AlmKeys.Azure, url: 'url' }),
    mockAlmSettingsInstance({
      key: 'conf-bitbucketcloud-1',
      alm: AlmKeys.BitbucketCloud,
      url: 'url',
    }),
    mockAlmSettingsInstance({
      key: 'conf-bitbucketcloud-2',
      alm: AlmKeys.BitbucketCloud,
      url: 'url',
    }),
    mockAlmSettingsInstance({
      key: 'conf-bitbucketserver-1',
      alm: AlmKeys.BitbucketServer,
      url: 'url',
    }),
    mockAlmSettingsInstance({
      key: 'conf-bitbucketserver-2',
      alm: AlmKeys.BitbucketServer,
      url: 'url',
    }),
  ];

  constructor() {
    this.almSettings = cloneDeep(this.defaultSetting);
    jest.mocked(getAlmSettings).mockImplementation(this.getAlmSettingsHandler);
  }

  getAlmSettingsHandler = () => {
    return Promise.resolve(this.almSettings);
  };

  reset = () => {
    this.almSettings = cloneDeep(this.defaultSetting);
  };
}
