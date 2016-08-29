/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import debounce from 'lodash/debounce';
import { defaultInputPropTypes } from '../../propTypes';

export default class SimpleInput extends React.Component {
  static propTypes = {
    ...defaultInputPropTypes,
    value: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.number]),
    type: React.PropTypes.string.isRequired,
    className: React.PropTypes.string.isRequired
  };

  constructor (props) {
    super(props);
    this.state = { value: props.value };
    this.handleChange = debounce(this.handleChange.bind(this), 250);
  }

  componentWillReceiveProps (nextProps) {
    if (this.props.isDefault !== nextProps.isDefault) {
      this.setState({ value: nextProps.value });
    }
  }

  handleInputChange (e) {
    if (e.target.validity.valid) {
      const { value } = e.target;
      this.setState({ value });
      this.handleChange(value);
    }
  }

  handleChange (value) {
    this.props.onChange(this.props.setting, value);
  }

  render () {
    return (
        <input
            name={this.props.name}
            className={this.props.className + ' text-top'}
            type={this.props.type}
            value={this.state.value}
            step="any"
            onChange={e => this.handleInputChange(e)}/>
    );
  }
}
