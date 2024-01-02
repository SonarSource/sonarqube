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
import { throwGlobalError } from '../helpers/error';
import { getJSON, post } from '../helpers/request';
import { Branch, PullRequest } from '../types/branch-like';

export function getBranches(project: string): Promise<Branch[]> {
  return getJSON('/api/project_branches/list', { project }).then(
    (r) => r.branches,
    throwGlobalError,
  );
}

export function getPullRequests(project: string): Promise<PullRequest[]> {
  return getJSON('/api/project_pull_requests/list', { project }).then(
    (r) => r.pullRequests,
    throwGlobalError,
  );
}

export function deleteBranch(data: { branch: string; project: string }) {
  return post('/api/project_branches/delete', data).catch(throwGlobalError);
}

export function deletePullRequest(data: { project: string; pullRequest: string }) {
  return post('/api/project_pull_requests/delete', data).catch(throwGlobalError);
}

export function renameBranch(project: string, name: string) {
  return post('/api/project_branches/rename', { project, name }).catch(throwGlobalError);
}

export function excludeBranchFromPurge(projectKey: string, branchName: string, excluded: boolean) {
  return post('/api/project_branches/set_automatic_deletion_protection', {
    project: projectKey,
    branch: branchName,
    value: excluded,
  }).catch(throwGlobalError);
}

export function setMainBranch(project: string, branch: string) {
  return post('/api/project_branches/set_main', { project, branch }).catch(throwGlobalError);
}
