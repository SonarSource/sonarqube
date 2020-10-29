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
  AlmSettingsBindingStatus,
  AlmSettingsBindingStatusType,
  AlmSettingsInstance,
  AzureBindingDefinition,
  BitbucketBindingDefinition,
  GithubBindingDefinition,
  GitlabBindingDefinition,
  ProjectAlmBindingResponse,
  ProjectBitbucketBindingResponse,
  ProjectGitHubBindingResponse,
  ProjectGitLabBindingResponse
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

export function mockAzureBindingDefinition(
  overrides: Partial<AzureBindingDefinition> = {}
): AzureBindingDefinition {
  return {
    key: 'key',
    personalAccessToken: 'asdf1234',
    ...overrides
  };
}

export function mockBitbucketBindingDefinition(
  overrides: Partial<BitbucketBindingDefinition> = {}
): BitbucketBindingDefinition {
  return {
    key: 'key',
    personalAccessToken: 'asdf1234',
    url: 'http://bbs.enterprise.com',
    ...overrides
  };
}

export function mockGithubBindingDefinition(
  overrides: Partial<GithubBindingDefinition> = {}
): GithubBindingDefinition {
  return {
    key: 'key',
    url: 'http://github.enterprise.com',
    appId: '123456',
    clientId: 'client1',
    clientSecret: '**clientsecret**',
    privateKey: 'asdf1234',
    ...overrides
  };
}

export function mockGitlabBindingDefinition(
  overrides: Partial<GitlabBindingDefinition> = {}
): GitlabBindingDefinition {
  return {
    key: 'foo',
    personalAccessToken: 'foobar',
    ...overrides
  };
}

export function mockProjectAlmBindingResponse(
  overrides: Partial<ProjectAlmBindingResponse> = {}
): ProjectAlmBindingResponse {
  return {
    alm: AlmKeys.GitHub,
    key: 'foo',
    ...overrides
  };
}

export function mockProjectBitbucketBindingResponse(
  overrides: Partial<ProjectBitbucketBindingResponse> = {}
): ProjectBitbucketBindingResponse {
  return {
    alm: AlmKeys.Bitbucket,
    key: 'foo',
    repository: 'PROJECT_KEY',
    slug: 'repo-slug',
    ...overrides
  };
}

export function mockProjectGithubBindingResponse(
  overrides: Partial<ProjectGitHubBindingResponse> = {}
): ProjectGitHubBindingResponse {
  return {
    alm: AlmKeys.GitHub,
    key: 'foo',
    repository: 'PROJECT_KEY',
    ...overrides
  };
}

export function mockProjectGitLabBindingResponse(
  overrides: Partial<ProjectGitLabBindingResponse> = {}
): ProjectGitLabBindingResponse {
  return {
    alm: AlmKeys.GitLab,
    key: 'foo',
    repository: 'PROJECT_KEY',
    url: 'https://gitlab.com/api/v4',
    ...overrides
  };
}

export function mockAlmSettingsBindingStatus(
  overrides: Partial<AlmSettingsBindingStatus>
): AlmSettingsBindingStatus {
  return {
    alertSuccess: false,
    failureMessage: '',
    type: AlmSettingsBindingStatusType.Validating,
    ...overrides
  };
}
