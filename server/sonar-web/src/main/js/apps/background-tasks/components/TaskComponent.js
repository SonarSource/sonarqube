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
import { Link } from 'react-router';
import TaskType from './TaskType';
import QualifierIcon from '../../../components/shared/qualifier-icon';
import { Task } from '../types';

const TaskComponent = ({ task, types }: { task: Task, types: string[] }) => {
  if (!task.componentKey) {
    return (
        <td>
          <span className="note">{task.id}</span>
          {types.length > 1 && (
              <TaskType task={task}/>
          )}
        </td>
    );
  }

  return (
      <td>
        <Link to={{ pathname: '/dashboard', query: { id: task.componentKey } }} className="link-with-icon">
          <span className="little-spacer-right">
            <QualifierIcon qualifier={task.componentQualifier}/>
          </span>
          <span>{task.componentName}</span>
        </Link>
        {types.length > 1 && (
            <TaskType task={task}/>
        )}
      </td>
  );
};

export default TaskComponent;
