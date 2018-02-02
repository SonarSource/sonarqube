/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import TaskType from './TaskType';
import { Task } from '../types';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import Organization from '../../../components/shared/Organization';
import {
  getProjectUrl,
  getShortLivingBranchUrl,
  getLongLivingBranchUrl,
  getPullRequestUrl
} from '../../../helpers/urls';
import ShortLivingBranchIcon from '../../../components/icons-components/ShortLivingBranchIcon';
import LongLivingBranchIcon from '../../../components/icons-components/LongLivingBranchIcon';
import PullRequestIcon from '../../../components/icons-components/PullRequestIcon';
import Tooltip from '../../../components/controls/Tooltip';

interface Props {
  task: Task;
}

export default function TaskComponent({ task }: Props) {
  if (!task.componentKey) {
    return (
      <td>
        <span className="note">{task.id}</span>
        <TaskType type={task.type} />
      </td>
    );
  }

  return (
    <td>
      {task.branchType === 'SHORT' && <ShortLivingBranchIcon className="little-spacer-right" />}
      {task.branchType === 'LONG' && <LongLivingBranchIcon className="little-spacer-right" />}
      {task.pullRequest !== undefined && <PullRequestIcon className="little-spacer-right" />}

      {!task.branchType &&
        !task.pullRequest &&
        task.componentQualifier && (
          <span className="little-spacer-right">
            <QualifierIcon qualifier={task.componentQualifier} />
          </span>
        )}

      {task.organization && <Organization organizationKey={task.organization} />}

      {task.componentName && (
        <Link className="spacer-right" to={getTaskComponentUrl(task.componentKey, task)}>
          {task.componentName}

          {task.branch && (
            <span className="text-limited text-text-top" title={task.branch}>
              <span style={{ marginLeft: 5, marginRight: 5 }}>/</span>
              {task.branch}
            </span>
          )}

          {task.pullRequest && (
            <Tooltip overlay={task.pullRequestTitle}>
              <span className="text-limited text-text-top">
                <span style={{ marginLeft: 5, marginRight: 5 }}>/</span>
                {task.pullRequest}
              </span>
            </Tooltip>
          )}
        </Link>
      )}

      <TaskType type={task.type} />
    </td>
  );
}

function getTaskComponentUrl(componentKey: string, task: Task) {
  if (task.branch && task.branchType === 'SHORT') {
    return getShortLivingBranchUrl(componentKey, task.branchType);
  } else if (task.branchType && task.branchType === 'LONG') {
    return getLongLivingBranchUrl(componentKey, task.branchType);
  } else if (task.pullRequest) {
    return getPullRequestUrl(componentKey, task.pullRequest);
  } else {
    return getProjectUrl(componentKey);
  }
}
