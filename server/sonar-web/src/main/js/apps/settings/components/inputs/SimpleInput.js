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
import { defaultInputPropTypes } from '../../propTypes';

export default class SimpleInput extends React.PureComponent {
  static propTypes = {
    ...defaultInputPropTypes,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    type: PropTypes.string.isRequired,
    className: PropTypes.string.isRequired,
    onCancel: PropTypes.func,
    onSave: PropTypes.func
  };

  handleInputChange = event => {
    this.props.onChange(event.currentTarget.value);
  };

  handleKeyDown = event => {
    if (event.keyCode === 13) {
      if (this.props.onSave) {
        this.props.onSave();
      }
    } else if (event.keyCode === 27) {
      if (this.props.onCancel) {
        this.props.onCancel();
      }
    }
  };

  render() {
    return (
      <input
        name={this.props.name}
        className={this.props.className + ' text-top'}
        type={this.props.type}
        value={this.props.value || ''}
        onChange={this.handleInputChange}
        onKeyDown={this.handleKeyDown}
      />
    );
  }
}
