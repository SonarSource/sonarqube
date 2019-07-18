/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import LongLivingBranchIcon from 'sonar-ui-common/components/icons/LongLivingBranchIcon';
import PullRequestIcon from 'sonar-ui-common/components/icons/PullRequestIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import ShortLivingBranchIcon from 'sonar-ui-common/components/icons/ShortLivingBranchIcon';
import Organization from '../../../components/shared/Organization';
import {
  getLongLivingBranchUrl,
  getProjectUrl,
  getPullRequestUrl,
  getShortLivingBranchUrl
} from '../../../helpers/urls';
import TaskType from './TaskType';

interface Props {
  task: T.Task;
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

      {!task.branchType && !task.pullRequest && task.componentQualifier && (
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
            <span className="text-limited text-text-top" title={task.pullRequestTitle}>
              <span style={{ marginLeft: 5, marginRight: 5 }}>/</span>
              {task.pullRequest}
            </span>
          )}
        </Link>
      )}

      <TaskType type={task.type} />
    </td>
  );
}

function getTaskComponentUrl(componentKey: string, task: T.Task) {
  if (task.branch) {
    if (task.branchType === 'SHORT') {
      return getShortLivingBranchUrl(componentKey, task.branch);
    } else if (task.branchType === 'LONG') {
      return getLongLivingBranchUrl(componentKey, task.branch);
    }
  } else if (task.pullRequest) {
    return getPullRequestUrl(componentKey, task.pullRequest);
  }
  return getProjectUrl(componentKey);
}
