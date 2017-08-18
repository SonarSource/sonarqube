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
import React from 'react';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import IncrementalBadge from './IncrementalBadge';
import PendingIcon from '../../../../components/shared/pending-icon';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export default function ComponentNavMeta(props) {
  const metaList = [];
  const canSeeBackgroundTasks = props.conf.showBackgroundTasks;
  const backgroundTasksUrl =
    window.baseUrl + `/project/background_tasks?id=${encodeURIComponent(props.component.key)}`;

  if (props.isInProgress) {
    const tooltip = canSeeBackgroundTasks
      ? translateWithParameters('component_navigation.status.in_progress.admin', backgroundTasksUrl)
      : translate('component_navigation.status.in_progress');
    metaList.push(
      <Tooltip
        key="isInProgress"
        overlay={<div dangerouslySetInnerHTML={{ __html: tooltip }} />}
        mouseLeaveDelay={2}>
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
      <Tooltip
        key="isPending"
        overlay={<div dangerouslySetInnerHTML={{ __html: tooltip }} />}
        mouseLeaveDelay={2}>
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
      <Tooltip
        key="isFailed"
        overlay={<div dangerouslySetInnerHTML={{ __html: tooltip }} />}
        mouseLeaveDelay={2}>
        <li>
          <span className="badge badge-danger">
            {translate('background_task.status.FAILED')}
          </span>
        </li>
      </Tooltip>
    );
  }
  if (props.analysisDate) {
    metaList.push(
      <li key="analysisDate">
        <DateTimeFormatter date={props.analysisDate} />
      </li>
    );
  }

  if (props.version) {
    metaList.push(
      <li key="version">
        Version {props.version}
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

  return (
    <div className="navbar-context-meta">
      <ul className="list-inline">
        {metaList}
      </ul>
    </div>
  );
}
