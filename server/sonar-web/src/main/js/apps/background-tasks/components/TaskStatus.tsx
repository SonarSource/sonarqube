/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import PendingIcon from '../../../components/icons/PendingIcon';
import { translate } from '../../../helpers/l10n';
import { TaskStatuses } from '../../../types/tasks';

interface Props {
  status: string;
}

export default function TaskStatus({ status }: Props) {
  let inner;

  switch (status) {
    case TaskStatuses.Pending:
      inner = <PendingIcon />;
      break;
    case TaskStatuses.InProgress:
      inner = <i className="spinner" />;
      break;
    case TaskStatuses.Success:
      inner = (
        <span className="badge badge-success">{translate('background_task.status.SUCCESS')}</span>
      );
      break;
    case TaskStatuses.Failed:
      inner = (
        <span className="badge badge-error">{translate('background_task.status.FAILED')}</span>
      );
      break;
    case TaskStatuses.Canceled:
      inner = <span className="badge">{translate('background_task.status.CANCELED')}</span>;
      break;
    default:
      inner = '';
  }

  return <td className="thin spacer-right">{inner}</td>;
}
