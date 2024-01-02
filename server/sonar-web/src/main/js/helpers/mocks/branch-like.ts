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
import { Branch, BranchLike, MainBranch, PullRequest } from '../../types/branch-like';

export function mockBranch(overrides: Partial<Branch> = {}): Branch {
  return {
    analysisDate: '2018-01-01',
    excludedFromPurge: true,
    isMain: false,
    name: 'branch-6.7',
    ...overrides,
  };
}

export function mockMainBranch(overrides: Partial<MainBranch> = {}): MainBranch {
  return mockBranch({
    isMain: true,
    name: 'master',
    ...overrides,
  }) as MainBranch;
}

export function mockPullRequest(overrides: Partial<PullRequest> = {}): PullRequest {
  return {
    analysisDate: '2018-01-01',
    base: 'master',
    branch: 'feature/foo/bar',
    key: '1001',
    target: 'master',
    title: 'Foo Bar feature',
    ...overrides,
  };
}

export function mockSetOfBranchAndPullRequest(): BranchLike[] {
  return [
    mockBranch({ name: 'branch-11' }),
    mockBranch({ name: 'branch-1' }),
    mockMainBranch(),
    mockPullRequest({ key: '1', title: 'PR-1' }),
    mockBranch({ name: 'branch-12' }),
    mockPullRequest({ key: '2', title: 'PR-2' }),
    mockBranch({ name: 'branch-3' }),
    mockBranch({ name: 'branch-2' }),
    mockPullRequest({ key: '2', title: 'PR-2', target: 'llb-100', isOrphan: true }),
  ];
}
