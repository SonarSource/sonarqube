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
 /* @flow */
import $ from 'jquery';
import moment from 'moment';
import React, { Component } from 'react';

import { DATE_FORMAT } from '../constants';

export default class DateFilter extends Component {
  componentDidMount () {
    this.attachDatePicker();
  }

  componentDidUpdate () {
    this.attachDatePicker();
  }

  attachDatePicker () {
    const opts = {
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true,
      onSelect: this.handleChange.bind(this)
    };
    if ($.fn && $.fn.datepicker) {
      $(this.refs.minDate).datepicker(opts);
      $(this.refs.maxDate).datepicker(opts);
    }
  }

  handleChange () {
    const date = {};
    const minDateRaw = this.refs.minDate.value;
    const maxDateRaw = this.refs.maxDate.value;
    const minDate = moment(minDateRaw, DATE_FORMAT, true);
    const maxDate = moment(maxDateRaw, DATE_FORMAT, true);

    if (minDate.isValid()) {
      date.minSubmittedAt = minDate.format(DATE_FORMAT);
    }

    if (maxDate.isValid()) {
      date.maxExecutedAt = maxDate.format(DATE_FORMAT);
    }

    this.props.onChange(date);
  }

  render () {
    const { minSubmittedAt, maxExecutedAt } = this.props;

    return (
        <div>
          <input
              className="input-small"
              value={minSubmittedAt}
              onChange={() => true}
              ref="minDate"
              type="text"
              placeholder="From"/>
          {' '}
          <input
              className="input-small"
              value={maxExecutedAt}
              onChange={() => true}
              ref="maxDate"
              type="text"
              placeholder="To"/>
        </div>
    );
  }
}
