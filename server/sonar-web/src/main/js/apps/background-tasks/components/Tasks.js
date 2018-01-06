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
/* @flow */
import React from 'react';
import classNames from 'classnames';
import Task from './Task';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  tasks: Array<*>,
  component: Object,
  loading: boolean,
  onCancelTask: Function,
  onFilterTask: Function
};
*/

/*::
type State = Object;
*/

export default class Tasks extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: State; */

  render() {
    const { tasks, component, loading, onCancelTask, onFilterTask } = this.props;

    const className = classNames('data zebra zebra-hover background-tasks', {
      'new-loading': loading
    });

    return (
      <div className="boxed-group boxed-group-inner">
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
                tasks={tasks}
                component={component}
                onCancelTask={onCancelTask}
                onFilterTask={onFilterTask}
                previousTask={index > 0 ? tasks[index - 1] : undefined}
              />
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}
