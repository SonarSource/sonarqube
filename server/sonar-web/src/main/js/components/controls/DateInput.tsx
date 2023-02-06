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
import { addMonths, setMonth, setYear, subMonths } from 'date-fns';
import { range } from 'lodash';
import * as React from 'react';
import { ActiveModifiers, DayPicker, Matcher } from 'react-day-picker';
import 'react-day-picker/dist/style.css';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { ButtonIcon, ClearButton } from '../../components/controls/buttons';
import OutsideClickHandler from '../../components/controls/OutsideClickHandler';
import CalendarIcon from '../../components/icons/CalendarIcon';
import ChevronLeftIcon from '../../components/icons/ChevronLeftIcon';
import ChevronRightIcon from '../../components/icons/ChevronRightIcon';
import {
  getMonthName,
  getShortMonthName,
  getShortWeekDayName,
  translate,
  translateWithParameters,
} from '../../helpers/l10n';
import './DateInput.css';
import EscKeydownHandler from './EscKeydownHandler';
import FocusOutHandler from './FocusOutHandler';
import Select from './Select';

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

const MONTHS_IN_YEAR = 12;
const YEARS_TO_DISPLAY = 10;

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

  handleDayClick = (day: Date, modifiers: ActiveModifiers) => {
    if (!modifiers.disabled) {
      this.closeCalendar();
      this.props.onChange(day);
    }
  };

  handleDayMouseEnter = (day: Date, modifiers: ActiveModifiers) => {
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

  getPreviousMonthAriaLabel = () => {
    const { currentMonth } = this.state;
    const previous = (currentMonth.getMonth() + MONTHS_IN_YEAR - 1) % MONTHS_IN_YEAR;

    return translateWithParameters(
      'show_month_x_of_year_y',
      getMonthName(previous),
      currentMonth.getFullYear() - Math.floor(previous / (MONTHS_IN_YEAR - 1))
    );
  };

  getNextMonthAriaLabel = () => {
    const { currentMonth } = this.state;

    const next = (currentMonth.getMonth() + MONTHS_IN_YEAR + 1) % MONTHS_IN_YEAR;

    return translateWithParameters(
      'show_month_x_of_year_y',
      getMonthName(next),
      currentMonth.getFullYear() + 1 - Math.ceil(next / (MONTHS_IN_YEAR - 1))
    );
  };

  render() {
    const {
      alignRight,
      highlightFrom,
      highlightTo,
      minDate,
      value: selectedDay,
      name,
      className,
      inputClassName,
      id,
      placeholder,
    } = this.props;
    const { lastHovered, currentMonth, open } = this.state;

    const after = this.props.maxDate || new Date();

    const years = range(new Date().getFullYear() - YEARS_TO_DISPLAY, new Date().getFullYear() + 1);
    const yearOptions = years.map((year) => ({ label: String(year), value: year }));
    const monthOptions = range(MONTHS_IN_YEAR).map((month) => ({
      label: getShortMonthName(month),
      value: month,
    }));

    let highlighted: Matcher = false;
    const lastHoveredOrValue = lastHovered || selectedDay;
    if (highlightFrom && lastHoveredOrValue) {
      highlighted = { from: highlightFrom, to: lastHoveredOrValue };
    }
    if (highlightTo && lastHoveredOrValue) {
      highlighted = { from: lastHoveredOrValue, to: highlightTo };
    }

    return (
      <FocusOutHandler onFocusOut={this.closeCalendar}>
        <OutsideClickHandler onClickOutside={this.closeCalendar}>
          <EscKeydownHandler onKeydown={this.closeCalendar}>
            <span className={classNames('date-input-control', className)}>
              <InputWrapper
                className={classNames('date-input-control-input', inputClassName, {
                  'is-filled': selectedDay !== undefined,
                })}
                id={id}
                innerRef={(node: HTMLInputElement | null) => (this.input = node)}
                name={name}
                onFocus={this.openCalendar}
                placeholder={placeholder}
                readOnly={true}
                type="text"
                value={selectedDay}
              />
              <CalendarIcon className="date-input-control-icon" fill="" />
              {selectedDay !== undefined && (
                <ClearButton
                  aria-label={translate('reset_date')}
                  className="button-tiny date-input-control-reset"
                  iconProps={{ size: 12 }}
                  onClick={this.handleResetClick}
                />
              )}
              {open && (
                <form className={classNames('date-input-calendar', { 'align-right': alignRight })}>
                  <fieldset
                    className="date-input-calendar-nav"
                    aria-label={translateWithParameters(
                      'date.select_month_and_year_x',
                      `${getMonthName(currentMonth.getMonth())}, ${currentMonth.getFullYear()}`
                    )}
                  >
                    <ButtonIcon
                      className="button-small"
                      aria-label={this.getPreviousMonthAriaLabel()}
                      onClick={this.handlePreviousMonthClick}
                    >
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
                    <ButtonIcon
                      className="button-small"
                      aria-label={this.getNextMonthAriaLabel()}
                      onClick={this.handleNextMonthClick}
                    >
                      <ChevronRightIcon />
                    </ButtonIcon>
                  </fieldset>
                  <DayPicker
                    mode="default"
                    disableNavigation={true}
                    components={{ CaptionLabel: () => null }}
                    disabled={{ after, before: minDate }}
                    weekStartsOn={1}
                    formatters={{
                      formatWeekdayName: (date) => getShortWeekDayName(date.getDay()),
                    }}
                    modifiers={{ highlighted }}
                    modifiersClassNames={{ highlighted: 'highlighted' }}
                    month={currentMonth}
                    selected={selectedDay}
                    onDayClick={this.handleDayClick}
                    onDayMouseEnter={this.handleDayMouseEnter}
                  />
                </form>
              )}
            </span>
          </EscKeydownHandler>
        </OutsideClickHandler>
      </FocusOutHandler>
    );
  }
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
