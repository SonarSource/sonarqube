/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { DatePicker } from './DatePicker';
import { LightLabel } from './Text';

interface DateRange {
  from?: Date;
  to?: Date;
}

interface Props {
  alignEndDateCalandarRight?: boolean;
  ariaNextMonthLabel: string;
  ariaPreviousMonthLabel: string;
  className?: string;
  clearButtonLabel: string;
  fromLabel: string;
  maxDate?: Date;
  minDate?: Date;
  onChange: (date: DateRange) => void;
  separatorText?: string;
  toLabel: string;
  value?: DateRange;
  valueFormatter: (date?: Date) => string;
}

export class DateRangePicker extends React.PureComponent<Props> {
  toDateInput?: HTMLInputElement | null;

  get from() {
    return this.props.value?.from;
  }

  get to() {
    return this.props.value?.to;
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
    const {
      alignEndDateCalandarRight,
      ariaNextMonthLabel,
      ariaPreviousMonthLabel,
      clearButtonLabel,
      fromLabel,
      minDate,
      maxDate,
      separatorText,
      toLabel,
      valueFormatter,
    } = this.props;

    return (
      <div className={classNames('sw-flex sw-items-center', this.props.className)}>
        <DatePicker
          ariaNextMonthLabel={ariaNextMonthLabel}
          ariaPreviousMonthLabel={ariaPreviousMonthLabel}
          clearButtonLabel={clearButtonLabel}
          currentMonth={this.to}
          data-test="from"
          highlightTo={this.to}
          id="date-from"
          maxDate={maxDate && this.to ? min([maxDate, this.to]) : maxDate ?? this.to}
          minDate={minDate}
          onChange={this.handleFromChange}
          placeholder={fromLabel}
          size="full"
          value={this.from}
          valueFormatter={valueFormatter}
        />
        <LightLabel className="sw-mx-2">{separatorText ?? '–'}</LightLabel>
        <DatePicker
          alignRight={alignEndDateCalandarRight}
          ariaNextMonthLabel={ariaNextMonthLabel}
          ariaPreviousMonthLabel={ariaPreviousMonthLabel}
          clearButtonLabel={clearButtonLabel}
          currentMonth={this.from}
          data-test="to"
          highlightFrom={this.from}
          id="date-to"
          inputRef={(element: HTMLInputElement | null) => {
            this.toDateInput = element;
          }}
          maxDate={maxDate}
          minDate={minDate && this.from ? max([minDate, this.from]) : minDate ?? this.from}
          onChange={this.handleToChange}
          placeholder={toLabel}
          size="full"
          value={this.to}
          valueFormatter={valueFormatter}
        />
      </div>
    );
  }
}
