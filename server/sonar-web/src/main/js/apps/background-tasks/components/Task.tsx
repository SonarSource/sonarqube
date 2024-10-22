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

import * as React from 'react';
import { TableRow } from '~design-system';
import { AppStateContext } from '../../../app/components/app-state/AppStateContext';
import { EditionKey } from '../../../types/editions';
import { Task as ITask } from '../../../types/tasks';
import TaskActions from './TaskActions';
import TaskComponent from './TaskComponent';
import TaskDate from './TaskDate';
import TaskExecutionTime from './TaskExecutionTime';
import TaskNodeName from './TaskNodeName';
import TaskStatus from './TaskStatus';
import TaskSubmitter from './TaskSubmitter';

interface Props {
  component?: unknown;
  onCancelTask: (task: ITask) => Promise<void>;
  onFilterTask: (task: ITask) => void;
  task: ITask;
  taskIndex: number;
}

export default function Task(props: Readonly<Props>) {
  const { task, component, taskIndex, onCancelTask, onFilterTask } = props;

  const appState = React.useContext(AppStateContext);
  const isDataCenter = appState.edition === EditionKey.datacenter;

  return (
    <TableRow>
      <TaskStatus status={task.status} />
      <TaskComponent task={task} />
      {isDataCenter && <TaskNodeName nodeName={task.nodeName} />}
      <TaskSubmitter submittedAt={task.submittedAt} submitter={task.submitterLogin} />
      <TaskDate baseDate={task.submittedAt} date={task.startedAt} />
      <TaskDate baseDate={task.submittedAt} date={task.executedAt} />
      <TaskExecutionTime ms={task.executionTimeMs} />
      <TaskActions
        component={component}
        taskIndex={taskIndex}
        onCancelTask={onCancelTask}
        onFilterTask={onFilterTask}
        task={task}
      />
    </TableRow>
  );
}
