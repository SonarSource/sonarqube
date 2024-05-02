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
import { BranchBase, PullRequestBase } from '~sonar-aligned/types/branch-like';
import { Status } from '~sonar-aligned/types/common';
import { NewCodeDefinition } from './new-code-definition';
import { QualityGateStatusCondition } from './quality-gates';

export interface Branch extends BranchBase {
  excludedFromPurge: boolean;
}

export interface MainBranch extends Branch {
  isMain: true;
}

export interface PullRequest extends PullRequestBase {}

export type BranchLike = Branch | PullRequest;

export interface BranchTree {
  branch: Branch;
  pullRequests: PullRequest[];
}

export interface BranchLikeTree {
  mainBranchTree?: BranchTree;
  branchTree: BranchTree[];
  parentlessPullRequests: PullRequest[];
  orphanPullRequests: PullRequest[];
}

export interface BranchWithNewCodePeriod extends Branch {
  newCodePeriod?: NewCodeDefinition;
}

export interface BranchStatusData {
  conditions?: QualityGateStatusCondition[];
  ignoredConditions?: boolean;
  status?: Status;
}
