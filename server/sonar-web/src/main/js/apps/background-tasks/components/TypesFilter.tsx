/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALL_TYPES } from '../constants';

interface Props {
  value: string;
  onChange: Function;
  types: string[];
}

export default class TypesFilter extends React.PureComponent<Props> {
  handleChange = ({ value }: { value: string }) => {
    this.props.onChange(value);
  };

  render() {
    const { value, types } = this.props;
    const options = types.map(t => {
      return {
        value: t,
        label: translate('background_task.type', t)
      };
    });

    const allOptions = [
      { value: ALL_TYPES, label: translate('background_task.type.ALL') },
      ...options
    ];

    return (
      <Select
        className="input-medium"
        clearable={false}
        onChange={this.handleChange}
        options={allOptions}
        searchable={false}
        value={value}
      />
    );
  }
}
