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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { format } from 'date-fns';
import * as React from 'react';
import { ActiveModifiers, Matcher, DayPicker as OriginalDayPicker } from 'react-day-picker';
import tw from 'twin.macro';
import { PopupPlacement, PopupZLevel, themeBorder, themeColor, themeContrast } from '../../helpers';
import { InputSizeKeys } from '../../types/theme';
import EscKeydownHandler from '../EscKeydownHandler';
import { FocusOutHandler } from '../FocusOutHandler';
import { InteractiveIcon } from '../InteractiveIcon';
import { OutsideClickHandler } from '../OutsideClickHandler';
import { CalendarIcon } from '../icons';
import { CloseIcon } from '../icons/CloseIcon';
import { Popup } from '../popups';
import { CustomCalendarNavigation } from './DatePickerCustomCalendarNavigation';
import { InputField } from './InputField';

// When no minDate is given, year dropdown will show year options up to PAST_MAX_YEARS in the past
const YEARS_TO_DISPLAY = 10;

interface Props {
  alignRight?: boolean;
  className?: string;
  clearButtonLabel: string;
  currentMonth?: Date;
  highlightFrom?: Date;
  highlightTo?: Date;
  id?: string;
  inputClassName?: string;
  inputRef?: React.Ref<HTMLInputElement>;
  maxDate?: Date;
  minDate?: Date;
  name?: string;
  onChange: (date: Date | undefined) => void;
  placeholder: string;
  showClearButton?: boolean;
  size?: InputSizeKeys;
  value?: Date;
  valueFormatter?: (date?: Date) => string;
  zLevel?: PopupZLevel;
}

interface State {
  currentMonth: Date;
  lastHovered?: Date;
  open: boolean;
}

function formatWeekdayName(date: Date) {
  return format(date, 'EEE'); // Short weekday name, e.g. Wed, Thu
}

export class DatePicker extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = { currentMonth: props.value ?? props.currentMonth ?? new Date(), open: false };
  }

  handleResetClick = () => {
    this.closeCalendar();
    this.props.onChange(undefined);
  };

  openCalendar = () => {
    this.setState({
      currentMonth: this.props.value ?? this.props.currentMonth ?? new Date(),
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
      clearButtonLabel,
      highlightFrom,
      highlightTo,
      inputRef,
      minDate,
      maxDate = new Date(),
      value: selectedDay,
      name,
      className,
      inputClassName,
      id,
      placeholder,
      showClearButton = true,
      valueFormatter = (date?: Date) => (date ? format(date, 'MMM d, yyyy') : ''),
      size,
      zLevel = PopupZLevel.Global,
    } = this.props;
    const { lastHovered, currentMonth, open } = this.state;

    // Infer start and end dropdown year from min/max dates, if set
    const fromYear = minDate ? minDate.getFullYear() : new Date().getFullYear() - YEARS_TO_DISPLAY;
    const toYear = maxDate.getFullYear();

    const selectedDays = selectedDay ? [selectedDay] : [];
    let highlighted: Matcher = false;
    const lastHoveredOrValue = lastHovered ?? selectedDay;

    if (highlightFrom && lastHoveredOrValue) {
      highlighted = { from: highlightFrom, to: lastHoveredOrValue };
      selectedDays.push(highlightFrom);
    }

    if (highlightTo && lastHoveredOrValue) {
      highlighted = { from: lastHoveredOrValue, to: highlightTo };
      selectedDays.push(highlightTo);
    }

    return (
      <FocusOutHandler onFocusOut={this.closeCalendar}>
        <OutsideClickHandler onClickOutside={this.closeCalendar}>
          <EscKeydownHandler onKeydown={this.closeCalendar}>
            <Popup
              allowResizing
              className="sw-overflow-visible" //Necessary for the month & year selectors
              overlay={
                open ? (
                  <div className={classNames('sw-p-2')}>
                    <DayPicker
                      captionLayout="dropdown-buttons"
                      className="sw-body-sm"
                      components={{
                        Caption: CustomCalendarNavigation,
                      }}
                      disabled={{ after: maxDate, before: minDate }}
                      formatters={{
                        formatWeekdayName,
                      }}
                      fromYear={fromYear}
                      mode="default"
                      modifiers={{ highlighted }}
                      modifiersClassNames={{ highlighted: 'rdp-highlighted' }}
                      month={currentMonth}
                      onDayClick={this.handleDayClick}
                      onDayMouseEnter={this.handleDayMouseEnter}
                      onMonthChange={(currentMonth) => {
                        this.setState({ currentMonth });
                      }}
                      selected={selectedDays}
                      toYear={toYear}
                      weekStartsOn={1}
                    />
                  </div>
                ) : null
              }
              placement={alignRight ? PopupPlacement.BottomRight : PopupPlacement.BottomLeft}
              zLevel={zLevel}
            >
              <span
                className={classNames('sw-relative sw-inline-block sw-cursor-pointer', className)}
              >
                <StyledInputField
                  aria-label={placeholder}
                  className={classNames(inputClassName, {
                    'is-filled': selectedDay !== undefined && showClearButton,
                  })}
                  id={id}
                  name={name}
                  onClick={this.openCalendar}
                  onFocus={this.openCalendar}
                  placeholder={placeholder}
                  readOnly
                  ref={inputRef}
                  size={size}
                  title={valueFormatter(selectedDay)}
                  type="text"
                  value={valueFormatter(selectedDay)}
                />

                <StyledCalendarIcon fill="datePickerIcon" />

                {selectedDay !== undefined && showClearButton && (
                  <StyledInteractiveIcon
                    Icon={CloseIcon}
                    aria-label={clearButtonLabel}
                    onClick={this.handleResetClick}
                    size="small"
                  />
                )}
              </span>
            </Popup>
          </EscKeydownHandler>
        </OutsideClickHandler>
      </FocusOutHandler>
    );
  }
}

const StyledCalendarIcon = styled(CalendarIcon)`
  ${tw`sw-absolute`};
  ${tw`sw-top-[0.625rem] sw-left-2`};
`;

const StyledInteractiveIcon = styled(InteractiveIcon)`
  ${tw`sw-absolute`};
  ${tw`sw-top-[0.375rem] sw-right-[0.375rem]`};
`;

const StyledInputField = styled(InputField)`
  input[type='text']& {
    ${tw`sw-pl-8`};
    ${tw`sw-cursor-pointer`};

    &.is-filled {
      ${tw`sw-pr-8`};
    }
  }
`;

const DayPicker = styled(OriginalDayPicker)`
  --rdp-cell-size: auto;
  /* Ensures the month/year dropdowns do not move on click, but rdp outline is not shown */
  --rdp-outline: 2px solid transparent;
  --rdp-outline-selected: 2px solid transparent;

  margin: 0;

  .rdp-head {
    color: ${themeContrast('datePicker')};
  }

  .rdp-day {
    height: 28px;
    width: 33px;
    border-radius: 0;
    color: ${themeContrast('datePickerDefault')};
  }

  /* Default modifiers */

  .rdp-day_disabled {
    cursor: not-allowed;
    background: ${themeColor('datePickerDisabled')};
    color: ${themeContrast('datePickerDisabled')};
  }

  .rdp-day:hover:not(.rdp-day_outside):not(.rdp-day_disabled):not(.rdp-day_selected) {
    background: ${themeColor('datePickerHover')};
    color: ${themeContrast('datePickerHover')};
  }

  .rdp-day:focus-visible {
    outline: ${themeBorder('focus', 'inputFocus')};
    background: inherit;
    z-index: 1;
  }

  .rdp-day.rdp-highlighted:not(.rdp-day_selected) {
    background: ${themeColor('datePickerRange')};
    color: ${themeContrast('datePickerRange')};
  }

  .rdp-day_selected,
  .rdp-day_selected:focus-visible {
    background: ${themeColor('datePickerSelected')};
    color: ${themeContrast('datePickerSelected')};
  }
`;
