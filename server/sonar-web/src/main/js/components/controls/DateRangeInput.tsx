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
import { max, min } from 'date-fns';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import DateInput from './DateInput';

type DateRange = { from?: Date; to?: Date };

interface Props {
  className?: string;
  maxDate?: Date;
  minDate?: Date;
  onChange: (date: DateRange) => void;
  value?: DateRange;
  alignEndDateCalandarRight?: boolean;
}

export default class DateRangeInput extends React.PureComponent<Props> {
  toDateInput?: DateInput | null;

  get from() {
    return this.props.value && this.props.value.from;
  }

  get to() {
    return this.props.value && this.props.value.to;
  }

  handleFromChange = (from: Date | undefined) => {
    this.props.onChange({ from, to: this.to });

    // use `setTimeout` to work around the immediate closing of the `toDateInput`
    setTimeout(() => {
      if (from && !this.to && this.toDateInput) {
        this.toDateInput.focus();
      }
    }, 0);
  };

  handleToChange = (to: Date | undefined) => {
    this.props.onChange({ from: this.from, to });
  };

  render() {
    const { alignEndDateCalandarRight, minDate, maxDate } = this.props;

    return (
      <div className={classNames('display-flex-end', this.props.className)}>
        <div className="display-flex-column">
          <label className="text-bold little-spacer-bottom" htmlFor="date-from">
            {translate('start_date')}
          </label>
          <DateInput
            currentMonth={this.to}
            data-test="from"
            id="date-from"
            highlightTo={this.to}
            minDate={minDate}
            maxDate={maxDate && this.to ? min([maxDate, this.to]) : maxDate || this.to}
            onChange={this.handleFromChange}
            placeholder={translate('start_date')}
            value={this.from}
          />
        </div>
        <span className="note little-spacer-left little-spacer-right little-spacer-bottom">
          {translate('to_')}
        </span>
        <div className="display-flex-column">
          <label className="text-bold little-spacer-bottom" htmlFor="date-to">
            {translate('end_date')}
          </label>
          <DateInput
            alignRight={alignEndDateCalandarRight}
            currentMonth={this.from}
            data-test="to"
            id="date-to"
            highlightFrom={this.from}
            minDate={minDate && this.from ? max([minDate, this.from]) : minDate || this.from}
            maxDate={maxDate}
            onChange={this.handleToChange}
            placeholder={translate('end_date')}
            ref={(element) => (this.toDateInput = element)}
            value={this.to}
          />
        </div>
      </div>
    );
  }
}
