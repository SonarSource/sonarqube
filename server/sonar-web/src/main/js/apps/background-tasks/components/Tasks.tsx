/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ContentCell, NumericalCell, Table, TableRow } from 'design-system';
import * as React from 'react';
import { AppStateContext } from '../../../app/components/app-state/AppStateContext';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { translate } from '../../../helpers/l10n';
import { EditionKey } from '../../../types/editions';
import { Task as ITask } from '../../../types/tasks';
import Task from './Task';

interface Props {
  tasks: ITask[];
  component?: unknown;
  onCancelTask: (task: ITask) => Promise<void>;
  onFilterTask: (task: ITask) => void;
}

const COLUMN_WIDTHS = [0, 'auto', 'auto', 0, 0, 0, 0];
const COLUMN_WIDTHS_WITH_NODES = [0, 'auto', 'auto', 0, 0, 0, 0, 0];

export function Tasks({ tasks, component, onCancelTask, onFilterTask }: Readonly<Props>) {
  const appState = React.useContext(AppStateContext);
  const isDataCenter = appState.edition === EditionKey.datacenter;

  return (
    <Table
      columnCount={isDataCenter ? COLUMN_WIDTHS_WITH_NODES.length : COLUMN_WIDTHS.length}
      columnWidths={isDataCenter ? COLUMN_WIDTHS_WITH_NODES : COLUMN_WIDTHS}
      header={
        <TableRow>
          <ContentCell>{translate('background_tasks.table.status')}</ContentCell>
          <ContentCell>{translate('background_tasks.table.task')}</ContentCell>
          {isDataCenter && (
            <ContentCell>{translate('background_tasks.table.nodeName')}</ContentCell>
          )}
          <ContentCell>{translate('background_tasks.table.submitted')}</ContentCell>
          <NumericalCell>{translate('background_tasks.table.started')}</NumericalCell>
          <NumericalCell>{translate('background_tasks.table.finished')}</NumericalCell>
          <NumericalCell>{translate('background_tasks.table.duration')}</NumericalCell>
          <ContentCell />
        </TableRow>
      }
    >
      {tasks.map((task) => (
        <Task
          component={component}
          key={task.id}
          onCancelTask={onCancelTask}
          onFilterTask={onFilterTask}
          task={task}
        />
      ))}
    </Table>
  );
}

export default withAppStateContext(Tasks);
