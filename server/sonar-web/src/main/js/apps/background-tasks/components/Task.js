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
import shallowCompare from 'react-addons-shallow-compare';
import TaskStatus from './TaskStatus';
import TaskComponent from './TaskComponent';
import TaskId from './TaskId';
import TaskDay from './TaskDay';
import TaskDate from './TaskDate';
import TaskExecutionTime from './TaskExecutionTime';
import TaskActions from './TaskActions';

export default class Task extends React.Component {
  static propTypes = {
    task: React.PropTypes.object.isRequired,
    index: React.PropTypes.number.isRequired,
    tasks: React.PropTypes.array.isRequired,
    component: React.PropTypes.object,
    types: React.PropTypes.array.isRequired,
    onCancelTask: React.PropTypes.func.isRequired,
    onFilterTask: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate(nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  render() {
    const { task, index, tasks, component, types, onCancelTask, onFilterTask } = this.props;

    const prevTask = index > 0 ? tasks[index - 1] : null;

    return (
      <tr>
        <TaskStatus task={task} />
        <TaskComponent task={task} types={types} />
        <TaskId task={task} />
        <TaskDay task={task} prevTask={prevTask} />
        <TaskDate date={task.submittedAt} baseDate={task.submittedAt} format="LTS" />
        <TaskDate date={task.startedAt} baseDate={task.submittedAt} format="LTS" />
        <TaskDate date={task.executedAt} baseDate={task.submittedAt} format="LTS" />
        <TaskExecutionTime task={task} />
        <TaskActions
          component={component}
          task={task}
          onFilterTask={onFilterTask}
          onCancelTask={onCancelTask}
        />
      </tr>
    );
  }
}
