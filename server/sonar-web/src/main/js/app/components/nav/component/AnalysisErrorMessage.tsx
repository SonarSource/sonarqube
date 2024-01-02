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
import { Link } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useLocation } from 'react-router-dom';
import { isBranch, isMainBranch, isPullRequest } from '../../../../helpers/branch-like';
import { hasMessage, translate } from '../../../../helpers/l10n';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { useBranchesQuery } from '../../../../queries/branch';
import { BranchLike } from '../../../../types/branch-like';
import { Task } from '../../../../types/tasks';
import { Component } from '../../../../types/types';

interface Props {
  component: Component;
  currentTask: Task;
  onLeave: () => void;
}

function isSameBranch(task: Task, branchLike?: BranchLike) {
  if (branchLike) {
    if (isMainBranch(branchLike)) {
      return (!task.pullRequest && !task.branch) || branchLike.name === task.branch;
    }
    if (isPullRequest(branchLike)) {
      return branchLike.key === task.pullRequest;
    }
    if (isBranch(branchLike)) {
      return branchLike.name === task.branch;
    }
  }
  return !task.branch && !task.pullRequest;
}

export function AnalysisErrorMessage(props: Props) {
  const { component, currentTask } = props;
  const { data: { branchLike } = {} } = useBranchesQuery(component);
  const currentTaskOnSameBranch = isSameBranch(currentTask, branchLike);

  const location = useLocation();

  const backgroundTaskUrl = getComponentBackgroundTaskUrl(component.key);
  const canSeeBackgroundTasks = component.configuration?.showBackgroundTasks;
  const isOnBackgroundTaskPage = location.pathname === backgroundTaskUrl.pathname;

  const branch =
    currentTask.branch ??
    `${currentTask.pullRequest ?? ''}${
      currentTask.pullRequestTitle ? ' - ' + currentTask.pullRequestTitle : ''
    }`;

  let messageKey;
  if (currentTaskOnSameBranch === false && branch) {
    messageKey = 'component_navigation.status.failed_branch';
  } else {
    messageKey = 'component_navigation.status.failed';
  }

  let type;
  if (hasMessage('background_task.type', currentTask.type)) {
    messageKey += '_X';
    type = translate('background_task.type', currentTask.type);
  }

  let url;
  let stacktrace;
  if (canSeeBackgroundTasks) {
    messageKey += '.admin';

    if (isOnBackgroundTaskPage) {
      messageKey += '.help';
      stacktrace = translate('background_tasks.show_stacktrace');
    } else {
      messageKey += '.link';
      url = (
        <Link onClick={props.onLeave} to={backgroundTaskUrl}>
          {translate('background_tasks.page')}
        </Link>
      );
    }
  }

  return (
    <FormattedMessage
      defaultMessage={translate(messageKey)}
      id={messageKey}
      values={{ branch, url, stacktrace, type }}
    />
  );
}
