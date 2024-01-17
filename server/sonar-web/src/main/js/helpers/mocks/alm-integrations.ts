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
  AzureProject,
  AzureRepository,
  BitbucketCloudRepository,
  BitbucketProject,
  BitbucketRepository,
  GithubRepository,
  GitlabProject,
} from '../../types/alm-integration';
import { GitlabConfiguration, ProvisioningType } from '../../types/provisioning';

export function mockAzureProject(overrides: Partial<AzureProject> = {}): AzureProject {
  return {
    name: 'azure-project-1',
    description: 'Azure Project',
    ...overrides,
  };
}

export function mockAzureRepository(overrides: Partial<AzureRepository> = {}): AzureRepository {
  return {
    name: 'Azure repo 1',
    projectName: 'Azure Project',
    ...overrides,
  };
}

export function mockBitbucketProject(overrides: Partial<BitbucketProject> = {}): BitbucketProject {
  return {
    id: 1,
    key: 'project',
    name: 'Project',
    ...overrides,
  };
}

export function mockBitbucketRepository(
  overrides: Partial<BitbucketRepository> = {},
): BitbucketRepository {
  return {
    id: 1,
    slug: 'project__repo',
    name: 'Repo',
    projectKey: 'project',
    ...overrides,
  };
}

export function mockBitbucketCloudRepository(
  overrides: Partial<BitbucketCloudRepository> = {},
): BitbucketCloudRepository {
  return {
    uuid: 1,
    slug: 'project__repo',
    name: 'Repo',
    projectKey: 'project',
    workspace: 'worksapce',
    ...overrides,
  };
}

export function mockGitHubRepository(overrides: Partial<GithubRepository> = {}): GithubRepository {
  return {
    id: 'id1234',
    key: 'key3456',
    name: 'repository 1',
    url: 'https://github.com/owner/repo1',
    ...overrides,
  };
}

export function mockGitlabProject(overrides: Partial<GitlabProject> = {}): GitlabProject {
  return {
    id: 'id1234',
    name: 'Awesome Project !',
    slug: 'awesome-project-exclamation',
    pathName: 'Company / Best Projects',
    pathSlug: 'company/best-projects',
    url: 'https://gitlab.company.com/best-projects/awesome-project-exclamation',
    ...overrides,
  };
}

export function mockGitlabConfiguration(
  overrides: Partial<GitlabConfiguration> = {},
): GitlabConfiguration {
  return {
    id: Math.random().toString(),
    enabled: false,
    url: 'URL',
    applicationId: '123',
    allowUsersToSignUp: false,
    synchronizeGroups: true,
    provisioningType: ProvisioningType.jit,
    provisioningGroups: ['Cypress Hill'],
    ...overrides,
  };
}
