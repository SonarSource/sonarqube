/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import SelectLegacy from '../../../components/controls/SelectLegacy';
import { translate } from '../../../helpers/l10n';
import { TaskStatuses } from '../../../types/tasks';
import { STATUSES } from '../constants';

interface Props {
  value?: string;
  onChange: (value?: string) => void;
}

export default class StatusFilter extends React.PureComponent<Props> {
  handleChange = ({ value }: { value: string }) => {
    this.props.onChange(value);
  };

  render() {
    const options = [
      { value: STATUSES.ALL, label: translate('background_task.status.ALL') },
      {
        value: STATUSES.ALL_EXCEPT_PENDING,
        label: translate('background_task.status.ALL_EXCEPT_PENDING')
      },
      { value: TaskStatuses.Pending, label: translate('background_task.status.PENDING') },
      { value: TaskStatuses.InProgress, label: translate('background_task.status.IN_PROGRESS') },
      { value: TaskStatuses.Success, label: translate('background_task.status.SUCCESS') },
      { value: TaskStatuses.Failed, label: translate('background_task.status.FAILED') },
      { value: TaskStatuses.Canceled, label: translate('background_task.status.CANCELED') }
    ];

    return (
      <SelectLegacy
        className="input-medium"
        clearable={false}
        onChange={this.handleChange}
        options={options}
        searchable={false}
        value={this.props.value}
      />
    );
  }
}
