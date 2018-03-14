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
import OutsideClickHandler from './OutsideClickHandler';
import * as theme from '../../app/theme';
import CalendarIcon from '../icons-components/CalendarIcon';
import ClearIcon from '../icons-components/ClearIcon';
import { longFormatterOption } from '../intl/DateFormatter';
import { ButtonIcon } from '../ui/buttons';
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

  state: State = { open: false };

  handleResetClick = () => {
    this.closeCalendar();
    this.props.onChange(undefined);
  };

  openCalendar = () => {
    this.setState({ open: true });
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

  render() {
    const { minDate, value } = this.props;
    const { formatDate } = this.context.intl;
    const formattedValue = value && formatDate(value, longFormatterOption);

    const after = this.props.maxDate || new Date();

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
              <DayPicker
                className="date-input-calendar"
                disabledDays={{ after, before: minDate }}
                onDayClick={this.handleDayClick}
                selectedDays={this.props.value}
              />
            )}
          </span>
        )}
      </OutsideClickHandler>
    );
  }
}
