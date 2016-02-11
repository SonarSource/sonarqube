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

import { getComponentUrl } from '../../../helpers/urls';
import QualifierIcon from '../../../components/shared/qualifier-icon';

export default function TaskComponent ({ task }) {
  if (!task.componentKey) {
    return (
        <td>
          <span className="note">{task.id}</span>
        </td>
    );
  }

  return (
      <td>
        <a className="link-with-icon" href={getComponentUrl(task.componentKey)}>
          <span className="little-spacer-right">
            <QualifierIcon qualifier={task.componentQualifier}/>
          </span>
          <span>{task.componentName}</span>
        </a>
      </td>
  );
}
