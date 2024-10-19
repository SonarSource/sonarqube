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
import { mockBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';

export function mockBranchList() {
  return [
    mockBranch({
      isMain: true,
      name: 'main',
      status: { qualityGateStatus: 'OK' },
    }),
    mockBranch({
      excludedFromPurge: false,
      name: 'delete-branch',
      analysisDate: '2018-01-30',
      status: { qualityGateStatus: 'ERROR' },
    }),
    mockBranch({ name: 'normal-branch', status: { qualityGateStatus: 'ERROR' } }),
  ];
}

export function mockPullRequestList() {
  return [
    mockPullRequest({
      title: 'TEST-191 update master',
      key: '01',
      status: { qualityGateStatus: 'OK' },
    }),
    mockPullRequest({
      title: 'TEST-192 update normal-branch',
      key: '02',
      analysisDate: '2018-01-30',
      base: 'normal-branch',
      target: 'normal-branch',
      status: { qualityGateStatus: 'ERROR' },
    }),
    mockPullRequest({
      title: 'TEST-193 dumb commit',
      key: '03',
      target: 'normal-branch',
      status: { qualityGateStatus: 'ERROR' },
    }),
  ];
}
