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

import { Dict } from './types';

export interface AzureProject {
  description: string;
  name: string;
}

export interface AzureRepository {
  name: string;
  projectName: string;
  sqProjectKey?: string;
  sqProjectName?: string;
}

export interface BitbucketProject {
  id: number;
  key: string;
  name: string;
}

export interface BitbucketRepository {
  id: number;
  name: string;
  projectKey: string;
  slug: string;
  sqProjectKey?: string;
}

export interface BitbucketCloudRepository {
  name: string;
  projectKey: string;
  slug: string;
  sqProjectKey?: string;
  uuid: number;
  workspace: string;
}

export type BitbucketProjectRepositories = Dict<{
  allShown: boolean;
  repositories: BitbucketRepository[];
}>;

export interface GithubOrganization {
  key: string;
  name: string;
}

export interface GithubRepository {
  id: string;
  key: string;
  name: string;
  sqProjectKey?: string;
  url: string;
}

export interface GitlabProject {
  id: string;
  name: string;
  pathName: string;
  pathSlug: string;
  slug: string;
  sqProjectKey?: string;
  sqProjectName?: string;
  url: string;
}
