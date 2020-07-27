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
  BitbucketProject,
  BitbucketRepository,
  GithubRepository,
  GitlabProject
} from '../../types/alm-integration';

export function mockBitbucketProject(overrides: Partial<BitbucketProject> = {}): BitbucketProject {
  return {
    id: 1,
    key: 'project',
    name: 'Project',
    ...overrides
  };
}

export function mockBitbucketRepository(
  overrides: Partial<BitbucketRepository> = {}
): BitbucketRepository {
  return {
    id: 1,
    slug: 'project__repo',
    name: 'Repo',
    projectKey: 'project',
    ...overrides
  };
}

export function mockGitHubRepository(overrides: Partial<GithubRepository> = {}): GithubRepository {
  return {
    id: 'id1234',
    key: 'key3456',
    name: 'repository 1',
    sqProjectKey: '',
    url: 'owner/repo1',
    ...overrides
  };
}

export function mockGitlabProject(overrides: Partial<GitlabProject> = {}): GitlabProject {
  return {
    id: 'id1234',
    name: 'Awesome Project !',
    slug: 'awesome-project-exclamation',
    pathName: 'Company / Best Projects',
    pathSlug: 'company/best-projects',
    sqProjectKey: '',
    url: 'https://gitlab.company.com/best-projects/awesome-project-exclamation',
    ...overrides
  };
}
