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
  AlmBindingDefinition,
  AlmKeys,
  BitbucketBindingDefinition,
  GithubBindingDefinition,
  ProjectAlmBindingResponse,
  ProjectBitbucketBindingResponse,
  ProjectGitHubBindingResponse,
  ProjectGitLabBindingResponse
} from '../types/alm-settings';

export function isProjectBitbucketBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectBitbucketBindingResponse {
  return binding.alm === AlmKeys.Bitbucket;
}

export function isProjectGitHubBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectGitHubBindingResponse {
  return binding.alm === AlmKeys.GitHub;
}

export function isProjectGitLabBindingResponse(
  binding: ProjectAlmBindingResponse
): binding is ProjectGitLabBindingResponse {
  return binding.alm === AlmKeys.GitLab;
}

export function isBitbucketBindingDefinition(
  binding?: AlmBindingDefinition & { url?: string; personalAccessToken?: string }
): binding is BitbucketBindingDefinition {
  return (
    binding !== undefined && binding.url !== undefined && binding.personalAccessToken !== undefined
  );
}

export function isGithubBindingDefinition(
  binding?: AlmBindingDefinition & { appId?: string; privateKey?: string; url?: string }
): binding is GithubBindingDefinition {
  return (
    binding !== undefined &&
    binding.appId !== undefined &&
    binding.privateKey !== undefined &&
    binding.url !== undefined
  );
}
