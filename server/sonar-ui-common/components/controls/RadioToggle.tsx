/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import './RadioToggle.css';
import Tooltip from './Tooltip';

type ToggleValueType = string | number | boolean;
interface Option {
  disabled?: boolean;
  label: string;
  tooltip?: string;
  value: ToggleValueType;
}

interface Props {
  className?: string;
  name: string;
  onCheck: (value: ToggleValueType) => void;
  options: Option[];
  value?: ToggleValueType;
}

export default class RadioToggle extends React.PureComponent<Props> {
  static defaultProps = {
    disabled: false,
    value: null,
  };

  renderOption = (option: Option) => {
    const checked = option.value === this.props.value;
    const htmlId = this.props.name + '__' + option.value;
    return (
      <li key={option.value.toString()}>
        <input
          checked={checked}
          disabled={option.disabled}
          id={htmlId}
          name={this.props.name}
          onChange={() => this.props.onCheck(option.value)}
          type="radio"
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
