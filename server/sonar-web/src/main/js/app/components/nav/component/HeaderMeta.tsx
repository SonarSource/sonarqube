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
import { TextMuted } from 'design-system';
import * as React from 'react';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import { isBranch, isPullRequest } from '../../../../helpers/branch-like';
import { translateWithParameters } from '../../../../helpers/l10n';
import { useBranchesQuery } from '../../../../queries/branch';
import { Task } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import { AnalysisStatus } from './AnalysisStatus';
import CurrentBranchLikeMergeInformation from './branch-like/CurrentBranchLikeMergeInformation';
import { getCurrentPage } from './utils';

export interface HeaderMetaProps {
  component: Component;
  currentUser: CurrentUser;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
}

export function HeaderMeta(props: HeaderMetaProps) {
  const { component, currentUser, currentTask, isInProgress, isPending } = props;

  const { data: { branchLike } = {} } = useBranchesQuery(component);

  const isABranch = isBranch(branchLike);

  const currentPage = getCurrentPage(component, branchLike);

  return (
    <div className="sw-flex sw-items-center sw-flex-shrink sw-min-w-0">
      <AnalysisStatus
        component={component}
        currentTask={currentTask}
        isInProgress={isInProgress}
        isPending={isPending}
      />
      {branchLike && <CurrentBranchLikeMergeInformation currentBranchLike={branchLike} />}
      {component.version !== undefined && isABranch && (
        <TextMuted
          text={translateWithParameters('version_x', component.version)}
          className="sw-ml-4 sw-whitespace-nowrap"
        />
      )}
      {isLoggedIn(currentUser) && currentPage !== undefined && !isPullRequest(branchLike) && (
        <HomePageSelect className="sw-ml-2" currentPage={currentPage} />
      )}
    </div>
  );
}

export default withCurrentUserContext(HeaderMeta);
