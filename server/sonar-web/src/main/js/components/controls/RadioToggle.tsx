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
import * as classNames from 'classnames';
import Tooltip from './Tooltip';

interface Option {
  disabled?: boolean;
  label: string;
  tooltip?: string;
  value: string;
}

interface Props {
  className?: string;
  name: string;
  onCheck: (value: string) => void;
  options: Option[];
  value?: string;
}

export default class RadioToggle extends React.PureComponent<Props> {
  static defaultProps = {
    disabled: false,
    value: null
  };

  handleChange = (e: React.SyntheticEvent<HTMLInputElement>) => {
    const newValue = e.currentTarget.value;
    this.props.onCheck(newValue);
  };

  renderOption = (option: Option) => {
    const checked = option.value === this.props.value;
    const htmlId = this.props.name + '__' + option.value;
    return (
      <li key={option.value}>
        <input
          checked={checked}
          disabled={option.disabled}
          id={htmlId}
          name={this.props.name}
          onChange={this.handleChange}
          type="radio"
          value={option.value}
        />
        <Tooltip overlay={option.tooltip || undefined}>
          <label htmlFor={htmlId}>{option.label}</label>
        </Tooltip>
      </li>
    );
  };

  render() {
    return (
      <ul className={classNames('radio-toggle', this.props.className)}>
        {this.props.options.map(this.renderOption)}
      </ul>
    );
  }
}
