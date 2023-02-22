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
import * as React from 'react';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import TutorialSelection from '../../../components/tutorials/TutorialSelection';
import { Alert } from '../../../components/ui/Alert';
import { getBranchLikeDisplayName, isBranch, isMainBranch } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ProjectAlmBindingResponse } from '../../../types/alm-settings';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { Component } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface EmptyOverviewProps {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component: Component;
  currentUser: CurrentUser;
  hasAnalyses?: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

export function EmptyOverview(props: EmptyOverviewProps) {
  const { branchLike, branchLikes, component, currentUser, hasAnalyses, projectBinding } = props;

  if (component.qualifier === ComponentQualifier.Application) {
    return (
      <div className="page page-limited">
        <Alert variant="warning" aria-label={translate('provisioning.no_analysis.application')}>
          {translate('provisioning.no_analysis.application')}
        </Alert>
      </div>
    );
  } else if (!isBranch(branchLike)) {
    return null;
  }

  const hasBranches = branchLikes.length > 1;
  const hasBadBranchConfig =
    branchLikes.length > 2 ||
    (branchLikes.length === 2 && branchLikes.some((branch) => isBranch(branch)));

  const showWarning = isMainBranch(branchLike) && hasBranches;
  const showTutorial = isMainBranch(branchLike) && !hasBranches && !hasAnalyses;

  let warning;
  if (isLoggedIn(currentUser) && showWarning && hasBadBranchConfig) {
    warning = translateWithParameters(
      'provisioning.no_analysis_on_main_branch.bad_configuration',
      getBranchLikeDisplayName(branchLike),
      translate('branches.main_branch')
    );
  } else {
    warning = translateWithParameters(
      'provisioning.no_analysis_on_main_branch',
      getBranchLikeDisplayName(branchLike)
    );
  }

  return (
    <div className="page page-limited">
      {isLoggedIn(currentUser) ? (
        <>
          {showWarning && (
            <Alert variant="warning" aria-label={warning}>
              {warning}
            </Alert>
          )}
          {showTutorial && (
            <TutorialSelection
              component={component}
              currentUser={currentUser}
              projectBinding={projectBinding}
              willRefreshAutomatically={true}
            />
          )}
        </>
      ) : (
        <Alert variant="warning" aria-label={warning}>
          {warning}
        </Alert>
      )}
    </div>
  );
}

export default withCurrentUserContext(EmptyOverview);
