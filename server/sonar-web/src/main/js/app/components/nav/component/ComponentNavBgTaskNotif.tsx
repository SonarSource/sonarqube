/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import NavBarNotif from '../../../../components/nav/NavBarNotif';
import PendingIcon from '../../../../components/icons-components/PendingIcon';
import { Component } from '../../../types';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { Task } from '../../../../api/ce';

interface Props {
  component: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
}

export default function ComponentNavBgTaskNotif({
  component,
  currentTask,
  isInProgress,
  isPending
}: Props) {
  const canSeeBackgroundTasks =
    component.configuration != undefined && component.configuration.showBackgroundTasks;
  const url = getComponentBackgroundTaskUrl(component.key);

  if (isInProgress) {
    return (
      <NavBarNotif className="alert alert-info">
        <i className="spinner spacer-right" />
        <span
          dangerouslySetInnerHTML={{
            __html: canSeeBackgroundTasks
              ? translateWithParameters('component_navigation.status.in_progress.admin', url)
              : translate('component_navigation.status.in_progress')
          }}
        />
      </NavBarNotif>
    );
  } else if (isPending) {
    return (
      <NavBarNotif className="alert alert-info">
        <PendingIcon className="spacer-right" />
        <span
          dangerouslySetInnerHTML={{
            __html: canSeeBackgroundTasks
              ? translateWithParameters('component_navigation.status.pending.admin', url)
              : translate('component_navigation.status.pending')
          }}
        />
      </NavBarNotif>
    );
  } else if (currentTask && currentTask.status === STATUSES.FAILED) {
    return (
      <NavBarNotif className="alert alert-danger">
        <span
          dangerouslySetInnerHTML={{
            __html: canSeeBackgroundTasks
              ? translateWithParameters('component_navigation.status.failed.admin', url)
              : translate('component_navigation.status.failed')
          }}
        />
      </NavBarNotif>
    );
  }
  return null;
}
