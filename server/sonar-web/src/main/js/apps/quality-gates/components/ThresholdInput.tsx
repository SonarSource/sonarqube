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
import Select from '../../../components/controls/Select';

interface Props {
  name: string;
  value: string;
  metric: T.Metric;
  onChange: (value: string) => void;
}

export default class ThresholdInput extends React.PureComponent<Props> {
  handleChange = (e: React.SyntheticEvent<HTMLInputElement>) => {
    this.props.onChange(e.currentTarget.value);
  };

  handleSelectChange = (option: { value: string } | null) => {
    if (option) {
      this.props.onChange(option.value);
    } else {
      this.props.onChange('');
    }
  };

  renderRatingInput() {
    const { name, value } = this.props;

    const options = [
      { label: 'A', value: '1' },
      { label: 'B', value: '2' },
      { label: 'C', value: '3' },
      { label: 'D', value: '4' }
    ];

    return (
      <Select
        className="input-tiny text-middle"
        clearable={true}
        id="condition-threshold"
        name={name}
        onChange={this.handleSelectChange}
        options={options}
        placeholder=""
        searchable={false}
        value={value}
      />
    );
  }

  render() {
    const { name, value, metric } = this.props;

    if (metric.type === 'RATING') {
      return this.renderRatingInput();
    }

    return (
      <input
        className="input-tiny text-middle"
        data-type={metric.type}
        id="condition-threshold"
        name={name}
        onChange={this.handleChange}
        type="text"
        value={value}
      />
    );
  }
}
