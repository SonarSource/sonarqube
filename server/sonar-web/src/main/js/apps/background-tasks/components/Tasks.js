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

import Task from './Task';
import { translate } from '../../../helpers/l10n';

export default function Tasks ({ tasks, component, types, onCancelTask, onFilterTask }) {
  return (
      <table className="data zebra zebra-hover background-tasks">
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
            <th>{translate('background_tasks.table.submitted')}</th>
            <th>{translate('background_tasks.table.started')}</th>
            <th>{translate('background_tasks.table.finished')}</th>
            <th>{translate('background_tasks.table.duration')}</th>
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
