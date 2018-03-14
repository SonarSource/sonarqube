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
import * as React from 'react';
import * as classNames from 'classnames';
import DayPicker, { DayModifiers } from 'react-day-picker';
import { intlShape, InjectedIntlProps } from 'react-intl';
import { range } from 'lodash';
import { addMonths, subMonths, setYear, setMonth } from 'date-fns';
import OutsideClickHandler from './OutsideClickHandler';
import Select from './Select';
import * as theme from '../../app/theme';
import CalendarIcon from '../icons-components/CalendarIcon';
import ChevronLeftIcon from '../icons-components/ChevronLeftIcon';
import ChevronRightIcon from '../icons-components/ChevronRightcon';
import ClearIcon from '../icons-components/ClearIcon';
import { longFormatterOption } from '../intl/DateFormatter';
import { ButtonIcon } from '../ui/buttons';
import { getShortMonthName } from '../../helpers/l10n';
import 'react-day-picker/lib/style.css';
import './styles.css';

interface Props {
  className?: string;
  inputClassName?: string;
  maxDate?: Date;
  minDate?: Date;
  name?: string;
  onChange: (date: Date | undefined) => void;
  placeholder: string;
  value?: Date;
}

interface State {
  currentMonth: Date;
  open: boolean;
}

// TODO calendar localization

export default class DateInput extends React.PureComponent<Props, State> {
  // prettier-ignore
  context!: InjectedIntlProps;
  input?: HTMLInputElement | null;

  static contextTypes = {
    intl: intlShape
  };

  state: State = { currentMonth: new Date(), open: false };

  handleResetClick = () => {
    this.closeCalendar();
    this.props.onChange(undefined);
  };

  openCalendar = () => {
    this.setState({ currentMonth: this.props.value || new Date(), open: true });
  };

  closeCalendar = () => {
    this.setState({ open: false });
  };

  handleDayClick = (day: Date, modifiers: DayModifiers) => {
    if (!modifiers.disabled) {
      this.closeCalendar();
      this.props.onChange(day);
    }
  };

  handleCurrentMonthChange = ({ value }: { value: number }) => {
    this.setState((state: State) => ({ currentMonth: setMonth(state.currentMonth, value) }));
  };

  handleCurrentYearChange = ({ value }: { value: number }) => {
    this.setState(state => ({ currentMonth: setYear(state.currentMonth, value) }));
  };

  handlePreviousMonthClick = () => {
    this.setState(state => ({ currentMonth: subMonths(state.currentMonth, 1) }));
  };

  handleNextMonthClick = () => {
    this.setState(state => ({ currentMonth: addMonths(state.currentMonth, 1) }));
  };

  render() {
    const { minDate, value } = this.props;
    const { formatDate } = this.context.intl;
    const formattedValue = value && formatDate(value, longFormatterOption);

    const after = this.props.maxDate || new Date();

    const months = range(12);
    const years = range(new Date().getFullYear() - 10, new Date().getFullYear() + 1);

    return (
      <OutsideClickHandler onClickOutside={this.closeCalendar}>
        {({ ref }) => (
          <span className={classNames('date-input-control', this.props.className)} ref={ref}>
            <input
              className={classNames('date-input-control-input', this.props.inputClassName)}
              name={this.props.name}
              onFocus={this.openCalendar}
              placeholder={this.props.placeholder}
              readOnly={true}
              ref={node => (this.input = node)}
              type="text"
              value={formattedValue || ''}
            />
            <CalendarIcon className="date-input-control-icon" fill="" />
            {this.props.value !== undefined && (
              <ButtonIcon
                className="button-tiny date-input-control-reset"
                color={theme.gray60}
                onClick={this.handleResetClick}>
                <ClearIcon size={12} />
              </ButtonIcon>
            )}
            {this.state.open && (
              <div className="date-input-calendar">
                <nav className="date-input-calendar-nav">
                  <ButtonIcon className="button-small" onClick={this.handlePreviousMonthClick}>
                    <ChevronLeftIcon />
                  </ButtonIcon>
                  <div className="date-input-calender-month">
                    <Select
                      className="date-input-calender-month-select"
                      onChange={this.handleCurrentMonthChange}
                      options={months.map(month => ({
                        label: getShortMonthName(month),
                        value: month
                      }))}
                      value={this.state.currentMonth.getMonth()}
                    />
                    <Select
                      className="date-input-calender-month-select spacer-left"
                      onChange={this.handleCurrentYearChange}
                      options={years.map(year => ({ label: String(year), value: year }))}
                      value={this.state.currentMonth.getFullYear()}
                    />
                  </div>
                  <ButtonIcon className="button-small" onClick={this.handleNextMonthClick}>
                    <ChevronRightIcon />
                  </ButtonIcon>
                </nav>
                <DayPicker
                  captionElement={<NullComponent />}
                  disabledDays={{ after, before: minDate }}
                  month={this.state.currentMonth}
                  navbarElement={<NullComponent />}
                  onDayClick={this.handleDayClick}
                  selectedDays={this.props.value}
                />
              </div>
            )}
          </span>
        )}
      </OutsideClickHandler>
    );
  }
}

function NullComponent() {
  return null;
}
