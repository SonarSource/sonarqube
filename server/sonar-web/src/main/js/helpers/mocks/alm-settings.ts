/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
  AlmKeys,
  AlmSettingsInstance,
  AzureBindingDefinition,
  BitbucketBindingDefinition,
  GithubBindingDefinition,
  GitlabBindingDefinition
} from '../../types/alm-settings';

export function mockAlmSettingsInstance(
  overrides: Partial<AlmSettingsInstance> = {}
): AlmSettingsInstance {
  return {
    alm: AlmKeys.GitHub,
    key: 'key',
    ...overrides
  };
}

export function mockAzureDefinition(
  overrides: Partial<AzureBindingDefinition> = {}
): AzureBindingDefinition {
  return {
    key: 'key',
    personalAccessToken: 'asdf1234',
    ...overrides
  };
}

export function mockBitbucketDefinition(
  overrides: Partial<BitbucketBindingDefinition> = {}
): BitbucketBindingDefinition {
  return {
    key: 'key',
    personalAccessToken: 'asdf1234',
    url: 'http://bbs.enterprise.com',
    ...overrides
  };
}

export function mockGithubDefinition(
  overrides: Partial<GithubBindingDefinition> = {}
): GithubBindingDefinition {
  return {
    key: 'key',
    url: 'http://github.enterprise.com',
    appId: '123456',
    privateKey: 'asdf1234',
    ...overrides
  };
}

export function mockGitlabDefinition(
  overrides: Partial<GitlabBindingDefinition> = {}
): GitlabBindingDefinition {
  return {
    key: 'foo',
    personalAccessToken: 'foobar',
    ...overrides
  };
}
