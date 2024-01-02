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
import classNames from 'classnames';
import * as React from 'react';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { DefaultSpecializedInputProps } from '../../utils';

export interface SimpleInputProps extends DefaultSpecializedInputProps {
  value: string | number;
}

export default class SimpleInput extends React.PureComponent<SimpleInputProps> {
  handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.props.onChange(event.currentTarget.value);
  };

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.nativeEvent.key === KeyboardKeys.Enter && this.props.onSave) {
      this.props.onSave();
    } else if (event.nativeEvent.key === KeyboardKeys.Escape && this.props.onCancel) {
      this.props.onCancel();
    }
  };

  render() {
    const { autoComplete, autoFocus, className, name, value = '', type } = this.props;
    return (
      <input
        autoComplete={autoComplete}
        autoFocus={autoFocus}
        className={classNames('text-top', className)}
        name={name}
        onChange={this.handleInputChange}
        onKeyDown={this.handleKeyDown}
        type={type}
        value={value}
      />
    );
  }
}
