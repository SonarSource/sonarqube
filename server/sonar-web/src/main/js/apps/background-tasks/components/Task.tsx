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
import TaskStatus from './TaskStatus';
import TaskComponent from './TaskComponent';
import TaskId from './TaskId';
import TaskDay from './TaskDay';
import TaskDate from './TaskDate';
import TaskExecutionTime from './TaskExecutionTime';
import TaskActions from './TaskActions';
import { Task as ITask } from '../types';

interface Props {
  component?: {};
  onCancelTask: (task: ITask) => void;
  onFilterTask: (task: ITask) => void;
  task: ITask;
  previousTask?: ITask;
}

export default function Task(props: Props) {
  const { task, component, onCancelTask, onFilterTask, previousTask } = props;

  return (
    <tr>
      <TaskStatus status={task.status} />
      <TaskComponent task={task} />
      <TaskId id={task.id} />
      <TaskDay
        submittedAt={task.submittedAt}
        prevSubmittedAt={previousTask && previousTask.submittedAt}
      />
      <TaskDate date={task.submittedAt} />
      <TaskDate date={task.startedAt} baseDate={task.submittedAt} />
      <TaskDate date={task.executedAt} baseDate={task.submittedAt} />
      <TaskExecutionTime ms={task.executionTimeMs} />
      <TaskActions
        component={component}
        task={task}
        onFilterTask={onFilterTask}
        onCancelTask={onCancelTask}
      />
    </tr>
  );
}
