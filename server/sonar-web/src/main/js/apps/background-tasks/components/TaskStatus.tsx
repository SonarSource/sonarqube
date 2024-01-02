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
import {
  ClockIcon,
  ContentCell,
  FlagErrorIcon,
  FlagSuccessIcon,
  FlagWarningIcon,
  Spinner,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { TaskStatuses } from '../../../types/tasks';

interface Props {
  status: string;
}

interface StatusDataDictionnary {
  [key: string]: StatusDataType;
}

interface StatusDataType {
  iconComponent: React.ReactElement;
  textKey: string;
}

const STATUS_ENUM: StatusDataDictionnary = {
  [TaskStatuses.Pending]: {
    iconComponent: <ClockIcon />,
    textKey: 'background_task.status.PENDING',
  },
  [TaskStatuses.InProgress]: {
    iconComponent: <Spinner />,
    textKey: 'background_task.status.IN_PROGRESS',
  },
  [TaskStatuses.Success]: {
    iconComponent: <FlagSuccessIcon />,
    textKey: 'background_task.status.SUCCESS',
  },
  [TaskStatuses.Failed]: {
    iconComponent: <FlagErrorIcon />,
    textKey: 'background_task.status.FAILED',
  },
  [TaskStatuses.Canceled]: {
    iconComponent: <FlagWarningIcon />,
    textKey: 'background_task.status.CANCELED',
  },
};

export default function TaskStatus({ status }: Readonly<Props>) {
  const statusData = STATUS_ENUM[status];

  if (!statusData) {
    return <ContentCell />;
  }

  return (
    <ContentCell>
      <div className="sw-flex sw-gap-1 sw-items-center">
        {statusData.iconComponent}
        {translate(statusData.textKey)}
      </div>
    </ContentCell>
  );
}
