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
import * as $ from 'jquery';
import * as React from 'react';
import * as classNames from 'classnames';
import { pick } from 'lodash';
import * as theme from '../../app/theme';
import ClearIcon from '../icons-components/ClearIcon';
import { ButtonIcon } from '../ui/buttons';
import './styles.css';

interface Props {
  className?: string;
  format?: string;
  inputClassName?: string;
  // see http://api.jqueryui.com/datepicker/#option-maxDate for details
  maxDate?: Date | string | number;
  minDate?: Date | string | number;
  name: string;
  onChange: (value?: string) => void;
  placeholder: string;
  value?: string;
}

export default class DateInput extends React.PureComponent<Props> {
  input?: HTMLInputElement | null;

  static defaultProps = {
    format: 'yy-mm-dd',
    maxDate: '+0'
  };

  componentDidMount() {
    this.attachDatePicker();
  }

  componentDidUpdate(prevProps: Props) {
    if ($.fn && ($.fn as any).datepicker && this.input) {
      if (prevProps.maxDate !== this.props.maxDate) {
        ($(this.input) as any).datepicker('option', { maxDate: this.props.maxDate });
      }
      if (prevProps.minDate !== this.props.minDate) {
        ($(this.input) as any).datepicker('option', { minDate: this.props.minDate });
      }
    }
  }

  handleChange = () => {
    if (this.input) {
      const { value } = this.input;
      this.props.onChange(value);
    }
  };

  handleResetClick = () => {
    this.props.onChange(undefined);
  };

  attachDatePicker() {
    const opts = {
      dateFormat: this.props.format,
      changeMonth: true,
      changeYear: true,
      maxDate: this.props.maxDate,
      minDate: this.props.minDate,
      onSelect: this.handleChange
    };

    if ($.fn && ($.fn as any).datepicker && this.input) {
      ($(this.input) as any).datepicker(opts);
    }
  }

  render() {
    const inputProps: { name?: string; placeholder?: string } = pick(this.props, [
      'placeholder',
      'name'
    ]);

    return (
      <span className={classNames('date-input-control', this.props.className)}>
        <input
          className={classNames('date-input-control-input', this.props.inputClassName)}
          onChange={this.handleChange}
          readOnly={true}
          ref={node => (this.input = node)}
          type="text"
          value={this.props.value || ''}
          {...inputProps}
        />
        <span className="date-input-control-icon">
          <svg width="14" height="14" viewBox="0 0 16 16">
            <path d="M5.5 6h2v2h-2V6zm3 0h2v2h-2V6zm3 0h2v2h-2V6zm-9 6h2v2h-2v-2zm3 0h2v2h-2v-2zm3 0h2v2h-2v-2zm-3-3h2v2h-2V9zm3 0h2v2h-2V9zm3 0h2v2h-2V9zm-9 0h2v2h-2V9zm11-9v1h-2V0h-7v1h-2V0h-2v16h15V0h-2zm1 15h-13V4h13v11z" />
          </svg>
        </span>
        {this.props.value !== undefined && (
          <ButtonIcon
            className="button-tiny date-input-control-reset"
            color={theme.gray60}
            onClick={this.handleResetClick}>
            <ClearIcon size={12} />
          </ButtonIcon>
        )}
      </span>
    );
  }
}
