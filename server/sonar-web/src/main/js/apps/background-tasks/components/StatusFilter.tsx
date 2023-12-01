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
import { InputSelect, LabelValueSelectOption } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { TaskStatuses } from '../../../types/tasks';
import { STATUSES } from '../constants';

interface StatusFilterProps {
  value?: string;
  id: string;
  onChange: (value?: string) => void;
}

export default function StatusFilter(props: Readonly<StatusFilterProps>) {
  const { id, value, onChange } = props;

  const options: LabelValueSelectOption<string>[] = [
    { value: STATUSES.ALL, label: translate('background_task.status.ALL') },
    {
      value: STATUSES.ALL_EXCEPT_PENDING,
      label: translate('background_task.status.ALL_EXCEPT_PENDING'),
    },
    { value: TaskStatuses.Pending, label: translate('background_task.status.PENDING') },
    { value: TaskStatuses.InProgress, label: translate('background_task.status.IN_PROGRESS') },
    { value: TaskStatuses.Success, label: translate('background_task.status.SUCCESS') },
    { value: TaskStatuses.Failed, label: translate('background_task.status.FAILED') },
    { value: TaskStatuses.Canceled, label: translate('background_task.status.CANCELED') },
  ];

  const handleChange = React.useCallback(
    ({ value }: LabelValueSelectOption<string>) => {
      onChange(value);
    },
    [onChange],
  );

  return (
    <InputSelect
      aria-labelledby="background-task-status-filter-label"
      className="sw-w-abs-200"
      id={id}
      onChange={handleChange}
      options={options}
      size="medium"
      value={options.find((o) => o.value === value)}
    />
  );
}
