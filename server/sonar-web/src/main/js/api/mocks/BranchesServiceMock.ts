/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockBranch, mockPullRequest } from '../../helpers/mocks/branch-like';
import { Branch, PullRequest } from '../../types/branch-like';
import {
  deleteBranch,
  deletePullRequest,
  excludeBranchFromPurge,
  getBranches,
  getPullRequests,
  renameBranch,
} from '../branches';

jest.mock('../branches');

const defaultBranches: Branch[] = [
  mockBranch({ isMain: true, name: 'main', status: { qualityGateStatus: 'OK' } }),
  mockBranch({
    excludedFromPurge: false,
    name: 'delete-branch',
    analysisDate: '2018-01-30',
    status: { qualityGateStatus: 'ERROR' },
  }),
  mockBranch({ name: 'normal-branch', status: { qualityGateStatus: 'ERROR' } }),
];

const defaultPullRequests: PullRequest[] = [
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

export default class BranchesServiceMock {
  branches: Branch[];
  pullRequests: PullRequest[];

  constructor() {
    this.branches = cloneDeep(defaultBranches);
    this.pullRequests = cloneDeep(defaultPullRequests);
    jest.mocked(getBranches).mockImplementation(this.getBranchesHandler);
    jest.mocked(getPullRequests).mockImplementation(this.getPullRequestsHandler);
    jest.mocked(deleteBranch).mockImplementation(this.deleteBranchHandler);
    jest.mocked(deletePullRequest).mockImplementation(this.deletePullRequestHandler);
    jest.mocked(renameBranch).mockImplementation(this.renameBranchHandler);
    jest.mocked(excludeBranchFromPurge).mockImplementation(this.excludeBranchFromPurgeHandler);
  }

  getBranchesHandler = () => {
    return this.reply(this.branches);
  };

  getPullRequestsHandler = () => {
    return this.reply(this.pullRequests);
  };

  deleteBranchHandler: typeof deleteBranch = ({ branch }) => {
    this.branches = this.branches.filter((b) => b.name !== branch);
    return this.reply({});
  };

  deletePullRequestHandler: typeof deletePullRequest = ({ pullRequest }) => {
    this.pullRequests = this.pullRequests.filter((b) => b.key !== pullRequest);
    return this.reply({});
  };

  renameBranchHandler: typeof renameBranch = (_, name) => {
    this.branches = this.branches.map((b) => (b.isMain ? { ...b, name } : b));
    return this.reply({});
  };

  excludeBranchFromPurgeHandler: typeof excludeBranchFromPurge = (_, name, value) => {
    this.branches = this.branches.map((b) =>
      b.name === name ? { ...b, excludedFromPurge: value } : b
    );
    return this.reply({});
  };

  emptyBranches = () => {
    this.branches = [];
  };

  emptyBranchesAndPullRequest = () => {
    this.branches = [];
    this.pullRequests = [];
  };

  addBranch = (branch: Branch) => {
    this.branches.push(branch);
  };

  addPullRequest = (branch: PullRequest) => {
    this.pullRequests.push(branch);
  };

  reset = () => {
    this.branches = cloneDeep(defaultBranches);
    this.pullRequests = cloneDeep(defaultPullRequests);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
