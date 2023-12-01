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
import styled from '@emotion/styled';
import {
  BranchIcon,
  ContentCell,
  Note,
  PullRequestIcon,
  QualifierIcon,
  StandoutLink,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import {
  getBranchUrl,
  getPortfolioUrl,
  getProjectUrl,
  getPullRequestUrl,
} from '../../../helpers/urls';
import { isPortfolioLike } from '../../../types/component';
import { Task } from '../../../types/tasks';

interface Props {
  task: Task;
}

export default function TaskComponent({ task }: Readonly<Props>) {
  return (
    <ContentCell>
      <div>
        <p>
          {task.componentKey && (
            <span className="sw-mr-2">
              <TaskComponentIndicator task={task} />

              {task.componentName && (
                <StandoutLink className="sw-ml-2" to={getTaskComponentUrl(task.componentKey, task)}>
                  <StyledSpan title={task.componentName}>{task.componentName}</StyledSpan>

                  {task.branch && (
                    <StyledSpan title={task.branch}>
                      <span className="sw-mx-1">/</span>
                      {task.branch}
                    </StyledSpan>
                  )}

                  {task.pullRequest && (
                    <StyledSpan title={task.pullRequestTitle}>
                      <span className="sw-mx-1">/</span>
                      {task.pullRequest}
                    </StyledSpan>
                  )}
                </StandoutLink>
              )}
            </span>
          )}

          <span>{translate('background_task.type', task.type)}</span>
        </p>

        <Note as="div" className="sw-mt-2">
          {translate('background_tasks.table.id')}: {task.id}
        </Note>
      </div>
    </ContentCell>
  );
}

function getTaskComponentUrl(componentKey: string, task: Task) {
  if (isPortfolioLike(task.componentQualifier)) {
    return getPortfolioUrl(componentKey);
  } else if (task.branch) {
    return getBranchUrl(componentKey, task.branch);
  } else if (task.pullRequest) {
    return getPullRequestUrl(componentKey, task.pullRequest);
  }
  return getProjectUrl(componentKey);
}

const StyledSpan = styled.span`
  display: inline-block;
  max-width: 16vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: -4px; /* compensate the inline-block effect on the wrapping link */
`;

function TaskComponentIndicator({ task }: Readonly<Props>) {
  if (task.branch !== undefined) {
    return <BranchIcon />;
  }

  if (task.pullRequest !== undefined) {
    return <PullRequestIcon />;
  }

  if (task.componentQualifier) {
    return <QualifierIcon qualifier={task.componentQualifier} />;
  }

  return null;
}
