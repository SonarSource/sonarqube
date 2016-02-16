/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import TaskStatus from './TaskStatus';
import TaskComponent from './TaskComponent';
import TaskDay from './TaskDay';
import TaskDate from './TaskDate';
import TaskExecutionTime from './TaskExecutionTime';
import TaskCancelButton from './TaskCancelButton';
import TaskLogsLink from './TaskLogsLink';
import { STATUSES } from './../constants';

export default function Task ({ task, index, tasks, component, onCancelTask, onFilterTask }) {
  function handleFilterTask (task, e) {
    e.preventDefault();
    onFilterTask(task);
  }

  const prevTask = index > 0 ? tasks[index - 1] : null;

  return (
      <tr>
        <TaskStatus task={task}/>
        <TaskComponent task={task}/>
        <TaskDay task={task} prevTask={prevTask}/>
        <TaskDate date={task.submittedAt} format="LTS"/>
        <TaskDate date={task.startedAt} format="LTS"/>
        <TaskDate date={task.executedAt} format="LTS"/>
        <TaskExecutionTime task={task}/>

        <td className="thin nowrap text-right">
          {task.logs && (
              <TaskLogsLink task={task}/>
          )}
          {task.status === STATUSES.PENDING && (
              <TaskCancelButton task={task} onCancelTask={onCancelTask}/>
          )}
        </td>

        <td className="thin nowrap">
          {!component && (
              <a
                  onClick={handleFilterTask.bind(this, task)}
                  className="icon-filter icon-half-transparent spacer-left"
                  href="#"
                  title={`Show only "${task.componentName}" tasks`}
                  data-toggle="tooltip"/>
          )}
        </td>
      </tr>
  );
}
