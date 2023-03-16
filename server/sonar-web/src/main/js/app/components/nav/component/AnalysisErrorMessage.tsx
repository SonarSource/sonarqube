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

import { Link } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useLocation } from 'react-router-dom';
import { hasMessage, translate } from '../../../../helpers/l10n';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { Task } from '../../../../types/tasks';
import { Component } from '../../../../types/types';

interface Props {
  component: Component;
  currentTask: Task;
  currentTaskOnSameBranch?: boolean;
  onLeave: () => void;
}

export function AnalysisErrorMessage(props: Props) {
  const { component, currentTask, currentTaskOnSameBranch } = props;

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
