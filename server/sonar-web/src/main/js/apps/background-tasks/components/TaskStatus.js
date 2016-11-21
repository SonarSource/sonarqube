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

import { STATUSES } from './../constants';
import PendingIcon from '../../../components/shared/pending-icon';
import { translate } from '../../../helpers/l10n';
import { Task } from '../types';

const TaskStatus = ({ task }: { task: Task }) => {
  let inner;

  switch (task.status) {
    case STATUSES.PENDING:
      inner = <PendingIcon/>;
      break;
    case STATUSES.IN_PROGRESS:
      inner = <i className="spinner"/>;
      break;
    case STATUSES.SUCCESS:
      inner = <span className="badge badge-success">{translate('background_task.status.SUCCESS')}</span>;
      break;
    case STATUSES.FAILED:
      inner = <span className="badge badge-danger">{translate('background_task.status.FAILED')}</span>;
      break;
    case STATUSES.CANCELED:
      inner = <span className="badge badge-muted">{translate('background_task.status.CANCELED')}</span>;
      break;
    default:
      inner = '';
  }

  return <td className="thin spacer-right">{inner}</td>;
};

export default TaskStatus;
