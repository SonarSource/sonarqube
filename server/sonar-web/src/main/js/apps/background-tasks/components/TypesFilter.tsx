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
import { InputSelect, LabelValueSelectOption } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { ALL_TYPES } from '../constants';

interface Props {
  id: string;
  onChange: Function;
  types: string[];
  value: string;
}

export default class TypesFilter extends React.PureComponent<Props> {
  handleChange = ({ value }: LabelValueSelectOption) => {
    this.props.onChange(value);
  };

  render() {
    const { value, types, id } = this.props;
    const options = types.map((t) => {
      return {
        value: t,
        label: translate('background_task.type', t),
      };
    });

    const allOptions: LabelValueSelectOption[] = [
      { value: ALL_TYPES, label: translate('background_task.type.ALL') },
      ...options,
    ];

    return (
      <InputSelect
        aria-labelledby="background-task-type-filter-label"
        className="sw-w-abs-200"
        id={id}
        isClearable={false}
        size="medium"
        onChange={this.handleChange}
        options={allOptions}
        value={allOptions.find((o) => o.value === value)}
      />
    );
  }
}
