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
/* @flow */
import React from 'react';
import { Link } from 'react-router';
import TaskType from './TaskType';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import Organization from '../../../components/shared/Organization';
/*:: import type { Task } from '../types'; */

/*::
type Props = {
  task: Task,
  types: Array<string>
};
*/

export default function TaskComponent(props /*: Props */) {
  const { task, types } = props;

  if (!task.componentKey) {
    return (
      <td>
        <span className="note">
          {task.id}
        </span>
        {types.length > 1 && <TaskType task={task} />}
      </td>
    );
  }

  return (
    <td>
      <span className="little-spacer-right">
        <QualifierIcon qualifier={task.componentQualifier} />
      </span>

      {task.organization != null && <Organization organizationKey={task.organization} />}

      <Link to={{ pathname: '/dashboard', query: { id: task.componentKey } }}>
        {task.componentName}
      </Link>

      {types.length > 1 && <TaskType task={task} />}
    </td>
  );
}
