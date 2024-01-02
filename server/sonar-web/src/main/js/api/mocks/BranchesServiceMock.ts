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
import { cloneDeep } from 'lodash';
import { Branch, PullRequest } from '../../types/branch-like';
import {
  deleteBranch,
  deletePullRequest,
  excludeBranchFromPurge,
  getBranches,
  getPullRequests,
  renameBranch,
  setMainBranch,
} from '../branches';
import { mockBranchList, mockPullRequestList } from './data/branches';

jest.mock('../branches');

export default class BranchesServiceMock {
  branches: Branch[];
  pullRequests: PullRequest[];

  constructor() {
    this.branches = mockBranchList();
    this.pullRequests = mockPullRequestList();

    jest.mocked(getBranches).mockImplementation(this.getBranchesHandler);
    jest.mocked(getPullRequests).mockImplementation(this.getPullRequestsHandler);
    jest.mocked(deleteBranch).mockImplementation(this.deleteBranchHandler);
    jest.mocked(deletePullRequest).mockImplementation(this.deletePullRequestHandler);
    jest.mocked(renameBranch).mockImplementation(this.renameBranchHandler);
    jest.mocked(excludeBranchFromPurge).mockImplementation(this.excludeBranchFromPurgeHandler);
    jest.mocked(setMainBranch).mockImplementation(this.setMainBranchHandler);
  }

  getBranchesHandler = () => {
    return this.reply(this.branches);
  };

  getPullRequestsHandler = () => {
    return this.reply(this.pullRequests);
  };

  deleteBranchHandler: typeof deleteBranch = ({ branch }) => {
    this.branches = this.branches.filter((b) => b.name !== branch);
    return this.reply(null);
  };

  deletePullRequestHandler: typeof deletePullRequest = ({ pullRequest }) => {
    this.pullRequests = this.pullRequests.filter((b) => b.key !== pullRequest);
    return this.reply(null);
  };

  renameBranchHandler: typeof renameBranch = (_, name) => {
    this.branches = this.branches.map((b) => (b.isMain ? { ...b, name } : b));
    return this.reply(null);
  };

  excludeBranchFromPurgeHandler: typeof excludeBranchFromPurge = (_, name, value) => {
    this.branches = this.branches.map((b) =>
      b.name === name ? { ...b, excludedFromPurge: value } : b,
    );
    return this.reply(null);
  };

  setMainBranchHandler: typeof setMainBranch = (_, branch) => {
    this.branches = this.branches.map((b) => ({
      ...b,
      excludedFromPurge: b.excludedFromPurge || b.isMain || b.name === branch,
      isMain: b.name === branch,
    }));
    return this.reply(null);
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
    this.branches = mockBranchList();
    this.pullRequests = mockPullRequestList();
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
