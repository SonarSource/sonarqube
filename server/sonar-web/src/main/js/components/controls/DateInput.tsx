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
import { addMonths, setMonth, setYear, subMonths } from 'date-fns';
import { range } from 'lodash';
import * as React from 'react';
import DayPicker, { DayModifiers, Modifier, Modifiers } from 'react-day-picker';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { ButtonIcon, ClearButton } from '../../components/controls/buttons';
import OutsideClickHandler from '../../components/controls/OutsideClickHandler';
import CalendarIcon from '../../components/icons/CalendarIcon';
import ChevronLeftIcon from '../../components/icons/ChevronLeftIcon';
import ChevronRightIcon from '../../components/icons/ChevronRightIcon';
import {
  getShortMonthName,
  getShortWeekDayName,
  getWeekDayName,
  translate,
} from '../../helpers/l10n';
import './DayPicker.css';
import EscKeydownHandler from './EscKeydownHandler';
import FocusOutHandler from './FocusOutHandler';
import Select from './Select';
import './styles.css';

interface Props {
  alignRight?: boolean;
  className?: string;
  currentMonth?: Date;
  highlightFrom?: Date;
  highlightTo?: Date;
  inputClassName?: string;
  maxDate?: Date;
  minDate?: Date;
  name?: string;
  id?: string;
  onChange: (date: Date | undefined) => void;
  placeholder: string;
  value?: Date;
}

interface State {
  currentMonth: Date;
  open: boolean;
  lastHovered?: Date;
}

type Week = [string, string, string, string, string, string, string];

export default class DateInput extends React.PureComponent<Props, State> {
  input?: HTMLInputElement | null;

  constructor(props: Props) {
    super(props);
    this.state = { currentMonth: props.value || props.currentMonth || new Date(), open: false };
  }

  focus = () => {
    if (this.input) {
      this.input.focus();
    }
    this.openCalendar();
  };

  handleResetClick = () => {
    this.closeCalendar();
    this.props.onChange(undefined);
  };

  openCalendar = () => {
    this.setState({
      currentMonth: this.props.value || this.props.currentMonth || new Date(),
      lastHovered: undefined,
      open: true,
    });
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

  handleDayMouseEnter = (day: Date, modifiers: DayModifiers) => {
    this.setState({ lastHovered: modifiers.disabled ? undefined : day });
  };

  handleCurrentMonthChange = ({ value }: { value: number }) => {
    this.setState((state: State) => ({ currentMonth: setMonth(state.currentMonth, value) }));
  };

  handleCurrentYearChange = ({ value }: { value: number }) => {
    this.setState((state) => ({ currentMonth: setYear(state.currentMonth, value) }));
  };

  handlePreviousMonthClick = () => {
    this.setState((state) => ({ currentMonth: subMonths(state.currentMonth, 1) }));
  };

  handleNextMonthClick = () => {
    this.setState((state) => ({ currentMonth: addMonths(state.currentMonth, 1) }));
  };

  render() {
    const {
      alignRight,
      highlightFrom,
      highlightTo,
      minDate,
      value,
      name,
      className,
      inputClassName,
      id,
      placeholder,
    } = this.props;
    const { lastHovered, currentMonth, open } = this.state;

    const after = this.props.maxDate || new Date();

    const months = range(12);
    const years = range(new Date().getFullYear() - 10, new Date().getFullYear() + 1);

    const selectedDays: Modifier[] = value ? [value] : [];
    let modifiers: Partial<Modifiers> | undefined;
    const lastHoveredOrValue = lastHovered || value;

    if (highlightFrom && lastHoveredOrValue) {
      modifiers = { highlighted: { from: highlightFrom, to: lastHoveredOrValue } };
      selectedDays.push(highlightFrom);
    }
    if (highlightTo && lastHoveredOrValue) {
      modifiers = { highlighted: { from: lastHoveredOrValue, to: highlightTo } };
      selectedDays.push(highlightTo);
    }

    const weekdaysLong = range(7).map(getWeekDayName) as Week;
    const weekdaysShort = range(7).map(getShortWeekDayName) as Week;

    const monthOptions = months.map((month) => ({
      label: getShortMonthName(month),
      value: month,
    }));
    const yearOptions = years.map((year) => ({ label: String(year), value: year }));

    return (
      <FocusOutHandler onFocusOut={this.closeCalendar}>
        <OutsideClickHandler onClickOutside={this.closeCalendar}>
          <EscKeydownHandler onKeydown={this.closeCalendar}>
            <span className={classNames('date-input-control', className)}>
              <InputWrapper
                className={classNames('date-input-control-input', inputClassName, {
                  'is-filled': value !== undefined,
                })}
                id={id}
                innerRef={(node: HTMLInputElement | null) => (this.input = node)}
                name={name}
                onFocus={this.openCalendar}
                placeholder={placeholder}
                readOnly={true}
                type="text"
                value={value}
              />
              <CalendarIcon className="date-input-control-icon" fill="" />
              {value !== undefined && (
                <ClearButton
                  aria-label={translate('reset_verb')}
                  className="button-tiny date-input-control-reset"
                  iconProps={{ size: 12 }}
                  onClick={this.handleResetClick}
                />
              )}
              {open && (
                <div className={classNames('date-input-calendar', { 'align-right': alignRight })}>
                  <nav className="date-input-calendar-nav">
                    <ButtonIcon className="button-small" onClick={this.handlePreviousMonthClick}>
                      <ChevronLeftIcon />
                    </ButtonIcon>
                    <div className="date-input-calender-month">
                      <Select
                        aria-label={translate('select_month')}
                        className="date-input-calender-month-select"
                        onChange={this.handleCurrentMonthChange}
                        options={monthOptions}
                        value={monthOptions.find(
                          (month) => month.value === currentMonth.getMonth()
                        )}
                      />
                      <Select
                        aria-label={translate('select_year')}
                        className="date-input-calender-month-select spacer-left"
                        onChange={this.handleCurrentYearChange}
                        options={yearOptions}
                        value={yearOptions.find(
                          (year) => year.value === currentMonth.getFullYear()
                        )}
                      />
                    </div>
                    <ButtonIcon className="button-small" onClick={this.handleNextMonthClick}>
                      <ChevronRightIcon />
                    </ButtonIcon>
                  </nav>
                  <DayPicker
                    captionElement={<NullComponent />}
                    disabledDays={{ after, before: minDate }}
                    firstDayOfWeek={1}
                    modifiers={modifiers}
                    month={currentMonth}
                    navbarElement={<NullComponent />}
                    onDayClick={this.handleDayClick}
                    onDayMouseEnter={this.handleDayMouseEnter}
                    selectedDays={selectedDays}
                    weekdaysLong={weekdaysLong}
                    weekdaysShort={weekdaysShort}
                  />
                </div>
              )}
            </span>
          </EscKeydownHandler>
        </OutsideClickHandler>
      </FocusOutHandler>
    );
  }
}

function NullComponent() {
  return null;
}

type InputWrapperProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, 'value'> &
  WrappedComponentProps & {
    innerRef: React.Ref<HTMLInputElement>;
    value: Date | undefined;
  };

const InputWrapper = injectIntl(({ innerRef, intl, value, ...other }: InputWrapperProps) => {
  const formattedValue =
    value && intl.formatDate(value, { year: 'numeric', month: 'short', day: 'numeric' });
  return <input {...other} ref={innerRef} value={formattedValue || ''} />;
});
