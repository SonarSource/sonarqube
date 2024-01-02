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
import { mockBranch } from '../../helpers/mocks/branch-like';
import { BranchLike } from '../../types/branch-like';
import { getBranches } from '../branches';

export default class BranchesServiceMock {
  branchLikes: BranchLike[];
  defaultBranchLikes: BranchLike[] = [
    mockBranch({ isMain: true, name: 'master' }),
    mockBranch({ excludedFromPurge: false, name: 'delete-branch' }),
    mockBranch({ name: 'normal-branch' }),
  ];

  constructor() {
    this.branchLikes = cloneDeep(this.defaultBranchLikes);
    (getBranches as jest.Mock).mockImplementation(this.getBranchesHandler);
  }

  getBranchesHandler = () => {
    return Promise.resolve(this.branchLikes);
  };

  resetBranches = () => {
    this.branchLikes = cloneDeep(this.defaultBranchLikes);
  };
}
