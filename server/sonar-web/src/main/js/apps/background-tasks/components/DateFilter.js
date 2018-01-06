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
import $ from 'jquery';
import React, { Component } from 'react';
import { isValidDate, parseDate, toShortNotSoISOString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';

export default class DateFilter extends Component {
  componentDidMount() {
    this.attachDatePicker();
  }

  componentDidUpdate() {
    this.attachDatePicker();
  }

  attachDatePicker() {
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

  handleChange() {
    const date = {};
    const minDate = parseDate(this.refs.minDate.value);
    const maxDate = parseDate(this.refs.maxDate.value);

    if (isValidDate(minDate)) {
      date.minSubmittedAt = toShortNotSoISOString(minDate);
    }

    if (isValidDate(maxDate)) {
      date.maxExecutedAt = toShortNotSoISOString(maxDate);
    }

    this.props.onChange(date);
  }

  render() {
    const { minSubmittedAt, maxExecutedAt } = this.props;

    return (
      <div className="nowrap">
        <input
          className="input-small"
          value={minSubmittedAt}
          onChange={() => true}
          ref="minDate"
          type="text"
          placeholder={translate('from')}
        />{' '}
        <input
          className="input-small"
          value={maxExecutedAt}
          onChange={() => true}
          ref="maxDate"
          type="text"
          placeholder={translate('to')}
        />
      </div>
    );
  }
}
