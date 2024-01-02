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
import * as React from 'react';
import { ActiveModifiers, DayPicker, Matcher } from 'react-day-picker';
import 'react-day-picker/dist/style.css';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { ClearButton } from '../../components/controls/buttons';
import OutsideClickHandler from '../../components/controls/OutsideClickHandler';
import CalendarIcon from '../../components/icons/CalendarIcon';
import { getShortWeekDayName, translate } from '../../helpers/l10n';
import './DateInput.css';
import EscKeydownHandler from './EscKeydownHandler';
import FocusOutHandler from './FocusOutHandler';

// When no minDate is given, year dropdown will show year options up to PAST_MAX_YEARS in the past
const YEARS_TO_DISPLAY = 10;

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

  render() {
    const {
      alignRight,
      highlightFrom,
      highlightTo,
      minDate,
      maxDate = new Date(),
      value: selectedDay,
      name,
      className,
      inputClassName,
      id,
      placeholder,
    } = this.props;
    const { lastHovered, currentMonth, open } = this.state;

    // Infer start and end dropdown year from min/max dates, if set
    const fromYear = minDate ? minDate.getFullYear() : new Date().getFullYear() - YEARS_TO_DISPLAY;
    const toYear = maxDate ? maxDate.getFullYear() : new Date().getFullYear() + 1;

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
                readOnly
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
                <div className={classNames('date-input-calendar', { 'align-right': alignRight })}>
                  <DayPicker
                    mode="default"
                    captionLayout="dropdown-buttons"
                    fromYear={fromYear}
                    toYear={toYear}
                    disabled={{ after: maxDate, before: minDate }}
                    weekStartsOn={1}
                    formatters={{
                      formatWeekdayName: (date) => getShortWeekDayName(date.getDay()),
                    }}
                    modifiers={{ highlighted }}
                    modifiersClassNames={{ highlighted: 'highlighted' }}
                    month={currentMonth}
                    onMonthChange={(currentMonth) => this.setState({ currentMonth })}
                    selected={selectedDay}
                    onDayClick={this.handleDayClick}
                    onDayMouseEnter={this.handleDayMouseEnter}
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

type InputWrapperProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, 'value'> &
  WrappedComponentProps & { innerRef: React.Ref<HTMLInputElement>; value: Date | undefined };

const InputWrapper = injectIntl(({ innerRef, intl, value, ...other }: InputWrapperProps) => {
  const formattedValue =
    value && intl.formatDate(value, { year: 'numeric', month: 'short', day: 'numeric' });
  return <input {...other} ref={innerRef} value={formattedValue || ''} />;
});
