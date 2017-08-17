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
import * as moment from 'moment';
import * as React from 'react';
import IncrementalBadge from './IncrementalBadge';
import BranchStatus from './BranchStatus';
import { Branch, Component, ComponentConfiguration } from '../../../types';
import Tooltip from '../../../../components/controls/Tooltip';
import PendingIcon from '../../../../components/icons-components/PendingIcon';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  branch: Branch;
  component: Component;
  conf: ComponentConfiguration;
  incremental?: boolean;
  isInProgress?: boolean;
  isFailed?: boolean;
  isPending?: boolean;
}

export default function ComponentNavMeta(props: Props) {
  const metaList = [];
  const canSeeBackgroundTasks = props.conf.showBackgroundTasks;
  const backgroundTasksUrl =
    (window as any).baseUrl +
    `/project/background_tasks?id=${encodeURIComponent(props.component.key)}`;

  if (props.isInProgress) {
    const tooltip = canSeeBackgroundTasks
      ? translateWithParameters('component_navigation.status.in_progress.admin', backgroundTasksUrl)
      : translate('component_navigation.status.in_progress');
    metaList.push(
      <Tooltip key="isInProgress" mouseLeaveDelay={2} overlay={tooltip}>
        <li>
          <i className="spinner" style={{ marginTop: '-1px' }} />{' '}
          <span className="text-info">{translate('background_task.status.IN_PROGRESS')}</span>
        </li>
      </Tooltip>
    );
  } else if (props.isPending) {
    const tooltip = canSeeBackgroundTasks
      ? translateWithParameters('component_navigation.status.pending.admin', backgroundTasksUrl)
      : translate('component_navigation.status.pending');
    metaList.push(
      <Tooltip key="isPending" mouseLeaveDelay={2} overlay={tooltip}>
        <li>
          <PendingIcon /> <span>{translate('background_task.status.PENDING')}</span>
        </li>
      </Tooltip>
    );
  } else if (props.isFailed) {
    const tooltip = canSeeBackgroundTasks
      ? translateWithParameters('component_navigation.status.failed.admin', backgroundTasksUrl)
      : translate('component_navigation.status.failed');
    metaList.push(
      <Tooltip key="isFailed" mouseLeaveDelay={2} overlay={tooltip}>
        <li>
          <span className="badge badge-danger">
            {translate('background_task.status.FAILED')}
          </span>
        </li>
      </Tooltip>
    );
  }

  if (props.component.analysisDate && props.branch.isMain) {
    metaList.push(
      <li key="analysisDate">
        {moment(props.component.analysisDate).format('LLL')}
      </li>
    );
  }

  if (props.component.version && props.branch.isMain) {
    metaList.push(
      <li key="version">
        Version {props.component.version}
      </li>
    );
  }

  if (props.incremental) {
    metaList.push(
      <li key="incremental">
        <IncrementalBadge />
      </li>
    );
  }

  if (!props.branch.isMain) {
    metaList.push(
      <li className="navbar-context-meta-branch" key="branch-status">
        <BranchStatus branch={props.branch} />
      </li>
    );
  }

  return (
    <div className="navbar-context-meta">
      <ul className="list-inline">
        {metaList}
      </ul>
    </div>
  );
}
