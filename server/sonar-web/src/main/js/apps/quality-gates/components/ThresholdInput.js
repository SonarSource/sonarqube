/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';

export default class ThresholdInput extends React.PureComponent {
  static propTypes = {
    name: PropTypes.string.isRequired,
    value: PropTypes.any,
    metric: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
  };

  handleChange = e => {
    this.props.onChange(e.target.value);
  };

  handleSelectChange = option => {
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

    const realValue = value === '' ? null : value;

    return (
      <Select
        className="input-tiny text-middle"
        name={name}
        value={realValue}
        options={options}
        searchable={false}
        placeholder=""
        onChange={this.handleSelectChange}
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
        name={name}
        type="text"
        className="input-tiny text-middle"
        value={value}
        data-type={metric.type}
        placeholder={metric.placeholder}
        onChange={this.handleChange}
      />
    );
  }
}
