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
import { AlmKeys } from '../../types/alm-settings';
import {
  isBitbucketBindingDefinition,
  isGithubBindingDefinition,
  isProjectBitbucketBindingResponse,
  isProjectGitHubBindingResponse
} from '../alm-settings';
import {
  mockBitbucketBindingDefinition,
  mockGithubBindingDefinition,
  mockProjectAlmBindingResponse,
  mockProjectBitbucketBindingResponse,
  mockProjectGithubBindingResponse
} from '../mocks/alm-settings';

/* eslint-disable sonarjs/no-duplicate-string */

describe('isProjectBitbucketBindingResponse', () => {
  it('works as expected', () => {
    expect(isProjectBitbucketBindingResponse(mockProjectAlmBindingResponse())).toBe(false);
    expect(isProjectBitbucketBindingResponse(mockProjectBitbucketBindingResponse())).toBe(true);
  });
});

describe('isBitbucketBindingDefinition', () => {
  it('works as expected', () => {
    expect(isBitbucketBindingDefinition(undefined)).toBe(false);
    expect(isBitbucketBindingDefinition(mockGithubBindingDefinition())).toBe(false);
    expect(isBitbucketBindingDefinition(mockBitbucketBindingDefinition())).toBe(true);
  });
});

describe('isProjectGithubBindingResponse', () => {
  it('works as expected', () => {
    expect(
      isProjectGitHubBindingResponse(mockProjectAlmBindingResponse({ alm: AlmKeys.Azure }))
    ).toBe(false);
    expect(isProjectGitHubBindingResponse(mockProjectGithubBindingResponse())).toBe(true);
  });
});

describe('isGithubBindingDefinition', () => {
  it('works as expected', () => {
    expect(isGithubBindingDefinition(undefined)).toBe(false);
    expect(isGithubBindingDefinition(mockBitbucketBindingDefinition())).toBe(false);
    expect(isGithubBindingDefinition(mockGithubBindingDefinition())).toBe(true);
  });
});
