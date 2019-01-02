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
import DateInput from './DateInput';
import { translate } from '../../helpers/l10n';

type DateRange = { from?: Date; to?: Date };

interface Props {
  className?: string;
  maxDate?: Date;
  minDate?: Date;
  onChange: (date: DateRange) => void;
  value?: DateRange;
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
    this.setState({ to });
    this.props.onChange({ from: this.from, to });
  };

  render() {
    return (
      <div className={classNames('display-inline-flex-center', this.props.className)}>
        <DateInput
          currentMonth={this.to}
          data-test="from"
          highlightTo={this.to}
          maxDate={this.to}
          onChange={this.handleFromChange}
          placeholder={translate('start_date')}
          value={this.from}
        />
        <span className="note little-spacer-left little-spacer-right">{translate('to_')}</span>
        <DateInput
          currentMonth={this.from}
          data-test="to"
          highlightFrom={this.from}
          minDate={this.from}
          onChange={this.handleToChange}
          placeholder={translate('end_date')}
          ref={element => (this.toDateInput = element)}
          value={this.to}
        />
      </div>
    );
  }
}
