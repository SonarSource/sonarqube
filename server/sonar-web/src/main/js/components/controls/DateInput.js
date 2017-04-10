/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import $ from 'jquery';
import React from 'react';
import { pick } from 'lodash';
import './styles.css';

export default class DateInput extends React.Component {
  static propTypes = {
    value: React.PropTypes.string,
    format: React.PropTypes.string,
    name: React.PropTypes.string,
    placeholder: React.PropTypes.string,
    onChange: React.PropTypes.func.isRequired
  };

  static defaultProps = {
    value: '',
    format: 'yy-mm-dd'
  };

  componentDidMount() {
    this.attachDatePicker();
  }

  componentWillReceiveProps(nextProps) {
    this.refs.input.value = nextProps.value;
  }

  handleChange() {
    const { value } = this.refs.input;
    this.props.onChange(value);
  }

  attachDatePicker() {
    const opts = {
      dateFormat: this.props.format,
      changeMonth: true,
      changeYear: true,
      onSelect: this.handleChange.bind(this)
    };

    if ($.fn && $.fn.datepicker) {
      $(this.refs.input).datepicker(opts);
    }
  }

  render() {
    const inputProps = pick(this.props, ['placeholder', 'name']);

    /* eslint max-len: 0 */
    return (
      <span className="date-input-control">
        <input
          className="date-input-control-input"
          ref="input"
          type="text"
          initialValue={this.props.value}
          readOnly={true}
          {...inputProps}
        />
        <span className="date-input-control-icon">
          <svg width="14" height="14" viewBox="0 0 16 16">
            <path
              d="M5.5 6h2v2h-2V6zm3 0h2v2h-2V6zm3 0h2v2h-2V6zm-9 6h2v2h-2v-2zm3 0h2v2h-2v-2zm3 0h2v2h-2v-2zm-3-3h2v2h-2V9zm3 0h2v2h-2V9zm3 0h2v2h-2V9zm-9 0h2v2h-2V9zm11-9v1h-2V0h-7v1h-2V0h-2v16h15V0h-2zm1 15h-13V4h13v11z"
            />
          </svg>
        </span>
      </span>
    );
  }
}
