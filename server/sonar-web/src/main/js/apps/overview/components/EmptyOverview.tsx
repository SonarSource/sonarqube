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

import { Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FlagMessage, LargeCenteredLayout, PageContentFontWrapper } from '~design-system';
import { isBranch, isMainBranch } from '~sonar-aligned/helpers/branch-like';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { getScannableProjects } from '../../../api/components';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { getBranchLikeDisplayName } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { useBranchesQuery } from '../../../queries/branch';
import { useTaskForComponentQuery } from '../../../queries/component';
import { BranchLike } from '../../../types/branch-like';
import { Permissions } from '../../../types/permissions';
import { TaskTypes } from '../../../types/tasks';
import { Component } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface EmptyOverviewProps {
  branchLike?: BranchLike;
  component: Component;
  currentUser: CurrentUser;
}

export function EmptyOverview(props: Readonly<EmptyOverviewProps>) {
  const { branchLike, component, currentUser } = props;

  const { data: branchLikes } = useBranchesQuery(component);

  const [currentUserCanScanProject, setCurrentUserCanScanProject] = React.useState(
    hasGlobalPermission(currentUser, Permissions.Scan),
  );

  const { data, isLoading } = useTaskForComponentQuery(component);

  const hasQueuedAnalyses =
    data && data.queue.filter((task) => task.type === TaskTypes.Report).length > 0;

  React.useEffect(() => {
    if (currentUserCanScanProject || !isLoggedIn(currentUser)) {
      return;
    }

    getScannableProjects()
      .then(({ projects }) => {
        setCurrentUserCanScanProject(projects.find((p) => p.key === component.key) !== undefined);
      })
      .catch(() => {
      });
  }, [component.key, currentUser, currentUserCanScanProject]);

  if (isLoading) {
    return <Spinner/>;
  }

  if (component.qualifier === ComponentQualifier.Application) {
    return (
      <LargeCenteredLayout className="sw-pt-8">
        <FlagMessage className="sw-w-full" variant="warning">
          {translate('provisioning.no_analysis.application')}
        </FlagMessage>
      </LargeCenteredLayout>
    );
  } else if (!isBranch(branchLike)) {
    return null;
  }

  const hasBranches = branchLikes.length > 1;

  const hasBadBranchConfig =
    branchLikes.length > 2 ||
    (branchLikes.length === 2 && branchLikes.some((branch) => isBranch(branch)));

  const showTutorial =
    currentUserCanScanProject && isMainBranch(branchLike) && !hasBranches && !hasQueuedAnalyses;

  if (showTutorial && isLoggedIn(currentUser)) {
    return (
      <LargeCenteredLayout className="sw-pt-8">
        <PageContentFontWrapper>
          <h1>{translate('onboarding.project_analysis.header')}</h1>
          <p className="sw-pt-4">
            {translate('layout.must_be_configured')}
            Run analysis on <Link to={`/project/extension/developer/project?id=${component.key}`}>Project
            Analysis</Link> Page.
          </p>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }

  let warning;

  if (isLoggedIn(currentUser) && isMainBranch(branchLike) && hasBranches && hasBadBranchConfig) {
    warning = translateWithParameters(
      'provisioning.no_analysis_on_main_branch.bad_configuration',
      getBranchLikeDisplayName(branchLike),
      translate('branches.main_branch'),
    );
  } else {
    warning = translateWithParameters(
      'provisioning.no_analysis_on_main_branch',
      getBranchLikeDisplayName(branchLike),
    );
  }

  return (
    <LargeCenteredLayout className="sw-pt-8">
      <PageContentFontWrapper>
        <FlagMessage className="sw-w-full" variant="warning">
          {warning}
        </FlagMessage>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withCurrentUserContext(EmptyOverview);
