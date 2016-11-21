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
 /* @flow */
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import classNames from 'classnames';

import Task from './Task';
import { translate } from '../../../helpers/l10n';

export default class Tasks extends React.Component {
  static propTypes = {
    tasks: React.PropTypes.array.isRequired,
    component: React.PropTypes.object,
    types: React.PropTypes.array.isRequired,
    loading: React.PropTypes.bool.isRequired,
    onCancelTask: React.PropTypes.func.isRequired,
    onFilterTask: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate (nextProps: any, nextState: any) {
    return shallowCompare(this, nextProps, nextState);
  }

  render () {
    const { tasks, component, types, loading, onCancelTask, onFilterTask } = this.props;

    const className = classNames('data zebra zebra-hover background-tasks', {
      'new-loading': loading
    });

    return (
        <table className={className}>
          <thead>
          <tr>
            <th>{translate('background_tasks.table.status')}</th>
            <th>{translate('background_tasks.table.task')}</th>
            <th>{translate('background_tasks.table.id')}</th>
            <th>&nbsp;</th>
            <th className="text-right">{translate('background_tasks.table.submitted')}</th>
            <th className="text-right">{translate('background_tasks.table.started')}</th>
            <th className="text-right">{translate('background_tasks.table.finished')}</th>
            <th className="text-right">{translate('background_tasks.table.duration')}</th>
            <th>&nbsp;</th>
          </tr>
          </thead>
          <tbody>
          {tasks.map((task, index, tasks) => (
              <Task
                  key={task.id}
                  task={task}
                  index={index}
                  tasks={tasks}
                  component={component}
                  types={types}
                  onCancelTask={onCancelTask}
                  onFilterTask={onFilterTask}/>
          ))}
          </tbody>
        </table>
    );
  }
}
